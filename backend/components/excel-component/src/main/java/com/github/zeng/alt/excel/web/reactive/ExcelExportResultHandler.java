package com.github.zeng.alt.excel.web.reactive;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.web.ExcelDownloadHeaders;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelRowTypeResolver;
import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
        applyHeaders(exchange, ExcelDownloadHeaders.of(annotation, returnType));

        return toRows(result.getReturnValue())
                .flatMap(rows -> writeTempFile(rows, rowType, annotation))
                .flatMap(temp -> sendAndDelete(exchange, temp));
    }

    // ==================== 返回值 → 行集合 ====================

    /**
     * 把各种返回值形状归一成一个行集合。
     * <p>
     * {@code Flux}/{@code Mono} 用 {@code collectList} 而不是阻塞拉取——在响应式栈里
     * 数据源本身就是非阻塞的，没有理由把它变成阻塞游标。{@code Flowable} 形状
     * 交给 {@link ExcelReactiveSupport} 转游标，那是可选依赖的边界。
     */
    private Mono<Collection<Object>> toRows(Object returnValue) {
        if (returnValue == null) {
            return Mono.just(List.of());
        }
        if (returnValue instanceof Flux<?> flux) {
            return flux.collectList().map(ExcelExportResultHandler::unchecked);
        }
        if (returnValue instanceof Mono<?> mono) {
            return mono.flatMap(value -> toRows(value)).defaultIfEmpty(List.of());
        }
        if (returnValue instanceof Collection<?> collection) {
            return Mono.just(unchecked(collection));
        }
        if (returnValue instanceof Iterator<?> iterator) {
            return Mono.just(drain(iterator));
        }
        if (reactiveSupport.supports(returnValue.getClass())) {
            return Mono.fromCallable(() -> drain(reactiveSupport.iterator(returnValue)
                            .getOrElseThrow(cause -> new ExcelWriteException("响应式返回值无法转成游标", cause))))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        return Mono.error(new ExcelWriteException(
                "@ExcelExport 只支持 Collection<T> / Iterator<T> / Mono<Collection<T>> / Flux<T> / Flowable<T> 返回值，实际是 "
                        + returnValue.getClass().getName()));
    }

    // ==================== 写出与下发 ====================

    private Mono<Path> writeTempFile(Collection<Object> rows, Class<?> rowType, ExcelExport annotation) {
        return Mono.fromCallable(() -> {
                    Path temp = ExcelUploadHelper.createTempFile(null);
                    try (OutputStream output = Files.newOutputStream(temp)) {
                        ExcelWriteSpec<Object> spec = specFactory.writeSpec(rowType, annotation);
                        spec.to(output)
                                .autoCloseStream(false)
                                .write(rows)
                                .getOrElseThrow(cause -> new ExcelWriteException("Excel 导出失败", cause));
                    }
                    return temp;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> sendAndDelete(ServerWebExchange exchange, Path temp) {
        exchange.getResponse().getHeaders().setContentLength(sizeOf(temp));
        return exchange.getResponse()
                .writeWith(DataBufferUtils.read(temp, exchange.getResponse().bufferFactory(), BUFFER_SIZE))
                .doFinally(signal -> ExcelUploadHelper.deleteQuietly(temp));
    }

    private static long sizeOf(Path temp) {
        try {
            return Files.size(temp);
        } catch (Exception e) {
            // 取不到就不设 Content-Length，交给分块传输
            return -1L;
        }
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

    private static Collection<Object> drain(Iterator<?> iterator) {
        List<Object> rows = new ArrayList<>();
        iterator.forEachRemaining(rows::add);
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> unchecked(Collection<?> collection) {
        return (Collection<Object>) collection;
    }
}
