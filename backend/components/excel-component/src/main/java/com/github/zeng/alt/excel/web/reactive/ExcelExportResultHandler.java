package com.github.zeng.alt.excel.web.reactive;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.context.ExcelContextHandler;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.web.ExcelDownloadHeaders;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelRowTypeResolver;
import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * WebFlux 栈：把 {@link ExcelExport} 标注的方法返回值写成 Excel 附件下发。
 * <p>
 * 支持 {@code Collection<T>}、{@code Iterator<T>}、{@code Mono<Collection<T>>}、
 * {@code Flux<T>} 与 {@code Flowable<T>}（需自行引入 RxJava）。
 * <p>
 * 与 Servlet 栈的两个关键差别：
 * <ul>
 *   <li><b>顺序靠 {@link Ordered} 就够。</b>WebFlux 的
 *       {@code ResponseBodyResultHandler} 的 order 是 100，本处理器取 0 即可抢先，
 *       不需要像 MVC 那样去改写 adapter 的处理器列表。</li>
 *   <li><b>先写临时文件，再零拷贝下发。</b>写 Excel 是阻塞动作且需要随机写
 *       （POI 的 xlsx 输出不是纯顺序流），直接往响应的 {@code DataBuffer} 上挤
 *       既会阻塞事件循环、也拿不到背压。所以在 {@code boundedElastic} 上写完临时
 *       文件，再用 {@code DataBufferUtils.read} 按缓冲区大小分块推给响应，
 *       内存占用与文件大小无关。</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelExportResultHandler implements HandlerResultHandler, Ordered {

    /** 分块推送响应体时每块的字节数 */
    private static final int BUFFER_SIZE = 8192;

    private final ExcelWebSpecFactory specFactory;
    private final ExcelReactiveSupport reactiveSupport;
    private final ExcelProperties properties;
    private final ExcelRowTypeResolver rowTypeResolver = new ExcelRowTypeResolver();

    @Override
    public int getOrder() {
        // ResponseBodyResultHandler 是 100，必须排在它前面
        return 0;
    }

    @Override
    public boolean supports(HandlerResult result) {
        return result.getReturnTypeSource().hasMethodAnnotation(ExcelExport.class);
    }

    @Override
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        MethodParameter returnType = result.getReturnTypeSource();
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        if (annotation == null) {
            return Mono.error(new ExcelWriteException("返回值上找不到 @ExcelExport"));
        }
        Class<?> rowType = resolveRowType(annotation, returnType);
        applyHeaders(exchange, ExcelDownloadHeaders.of(annotation, returnType, specFactory.fileNameTimestampPattern()));

        return ExcelReactiveContextHelper.capture(specFactory.captureContext(), properties.getContext().isEnabled())
                .flatMap(snapshot -> Mono.using(
                        () -> ExcelUploadHelper.createTempFile(properties.getWeb().getTempDir()),
                        temp -> writeTempFile(temp, result.getReturnValue(), rowType, annotation, snapshot)
                                .then(Mono.defer(() -> send(exchange, temp))),
                        ExcelUploadHelper::deleteQuietly
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Void> writeTempFile(
            Path temp,
            Object rows,
            Class<?> rowType,
            ExcelExport annotation,
            ExcelContextHandler.Snapshot snapshot
    ) {
        if (rows instanceof Mono<?> mono) {
            return mono.cast(Object.class).defaultIfEmpty(List.of())
                    .flatMap(value -> writeTempFile(temp, value, rowType, annotation, snapshot));
        }
        return writeRows(temp, rows, rowType, annotation, snapshot);
    }

    private Mono<Void> writeRows(
            Path temp,
            Object rows,
            Class<?> rowType,
            ExcelExport annotation,
            ExcelContextHandler.Snapshot snapshot
    ) {
        return Mono.<Void>fromCallable(() -> {
            try (ExcelContextHandler.Scope scope = snapshot.restore();
                 OutputStream output = Files.newOutputStream(temp)) {
                final ExcelWriteSpec<Object> spec = specFactory.writeSpec(rowType, annotation)
                        .to(output).autoCloseStream(false);
                write(spec, rows);
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @SuppressWarnings("unchecked")
    private void write(ExcelWriteSpec<Object> spec, Object rows) {
        if (rows instanceof Publisher<?> publisher
                && (rows instanceof Flux<?> || reactiveSupport.supports(rows.getClass()))) {
            // toStream 的预取固定为 64；关闭游标会取消上游，包括写出失败和请求取消。
            try (Stream<?> stream = Flux.from(publisher).toStream(64)) {
                spec.write((Iterator<Object>) stream.iterator())
                        .getOrElseThrow(cause -> new ExcelWriteException("Excel 导出失败", cause));
            }
            return;
        }
        final Iterator<?> iterator = switch (rows) {
            case null -> List.of().iterator();
            case Collection<?> collection -> collection.iterator();
            case Iterator<?> cursor -> cursor;
            default -> throw new ExcelWriteException("@ExcelExport 不支持返回类型: " + rows.getClass().getName());
        };
        spec.write((Iterator<Object>) iterator)
                .getOrElseThrow(cause -> new ExcelWriteException("Excel 导出失败", cause));
    }

    private Mono<Void> send(ServerWebExchange exchange, Path temp) {
        return Mono.fromCallable(() -> Files.size(temp))
                .flatMap(size -> {
                    exchange.getResponse().getHeaders().setContentLength(size);
                    return exchange.getResponse().writeWith(
                            DataBufferUtils.read(temp, exchange.getResponse().bufferFactory(), BUFFER_SIZE)
                    );
                });
    }

    // ==================== 杂项 ====================

    private static void applyHeaders(ServerWebExchange exchange, ExcelDownloadHeaders headers) {
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, headers.contentType());
        responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, headers.contentDisposition());
        responseHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ExcelDownloadHeaders.EXPOSE_HEADERS);
        responseHeaders.set(ExcelDownloadHeaders.FILE_NAME_HEADER, headers.fileName());
    }

    private Class<?> resolveRowType(ExcelExport annotation, MethodParameter returnType) {
        Class<?> explicit = ExcelWebSpecFactory.explicitRowType(annotation);
        if (explicit != null) {
            return explicit;
        }
        Class<?> rowType = rowTypeResolver.resolveOrNull(returnType);
        if (rowType == null) {
            throw new ExcelWriteException(
                    "无法从返回值推断导出实体类型，请显式指定 @ExcelExport(type = Xxx.class)");
        }
        return rowType;
    }

}
