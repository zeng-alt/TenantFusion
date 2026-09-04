package com.github.zeng.alt.excel.web.reactive;

import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelRowError;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelRowTypeResolver;
import com.github.zeng.alt.excel.web.ExcelStreamSource;
import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WebFlux 栈：把上传的 Excel 文件解析成 {@link ExcelImport} 标注的方法参数。
 * <p>
 * 支持四种形状：{@code List<T>}、{@link ExcelReadResult}{@code <T>}、
 * {@code Flux<T>}（Reactor 原生）与 {@code Flowable<T>}（需自行引入 RxJava）。
 * <p>
 * 两件 WebFlux 特有的事：
 * <ul>
 *   <li><b>不阻塞事件循环。</b>Excel 解析是阻塞动作，全部放到
 *       {@code Schedulers.boundedElastic()} 上。</li>
 *   <li><b>上传先落盘。</b>{@code FilePart} 的数据缓冲在请求结束后就被释放，
 *       解析（尤其是懒执行的流形状）必须基于落盘后的临时文件。</li>
 * </ul>
 * 这里用 Reactor 而不是 RxJava：WebFlux 的扩展点签名本身就是 {@code Mono}，
 * 属于「框架强加的 Reactor 留在框架层」，业务代码拿到的仍是集合或自己选的流类型。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelImportReactiveArgumentResolver implements HandlerMethodArgumentResolver {

    private final ExcelWebSpecFactory specFactory;
    private final ExcelProperties properties;
    private final ExcelReactiveSupport reactiveSupport;
    private final ExcelRowTypeResolver rowTypeResolver = new ExcelRowTypeResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelImport.class);
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
                                        ServerWebExchange exchange) {
        ExcelImport annotation = parameter.getParameterAnnotation(ExcelImport.class);
        if (annotation == null) {
            return Mono.error(new IllegalStateException(
                    "参数 '%s' 上没有 @ExcelImport".formatted(parameter.getParameterName())));
        }
        Class<?> container = parameter.getParameterType();
        if (!isSupportedContainer(container)) {
            return Mono.error(new IllegalArgumentException(
                    "@ExcelImport 只支持 List<T> / ExcelReadResult<T> / Flux<T> / Flowable<T>，参数 '%s' 是 %s"
                            .formatted(parameter.getParameterName(), container.getName())));
        }
        Class<?> rowType = rowTypeResolver.resolve(parameter, "@ExcelImport");
        return resolveParts(parameter, exchange, annotation)
                .flatMap(parts -> toArgument(parts, container, rowType, annotation));
    }

    /**
     * 参数容器是否是本解析器支持的形状。
     *
     * @param container 参数类型
     * @return true 表示支持
     */
    public boolean isSupportedContainer(Class<?> container) {
        return Collection.class.isAssignableFrom(container)
                || ExcelReadResult.class.isAssignableFrom(container)
                || Flux.class.isAssignableFrom(container)
                || reactiveSupport.supports(container);
    }

    // ==================== 取文件 ====================

    private Mono<List<FilePart>> resolveParts(MethodParameter parameter, ServerWebExchange exchange,
                                              ExcelImport annotation) {
        String name = StringUtils.hasText(annotation.value()) ? annotation.value() : parameter.getParameterName();
        return exchange.getMultipartData()
                .map(data -> filePartsOf(data, name))
                .defaultIfEmpty(List.of())
                .flatMap(parts -> parts.isEmpty() && annotation.required()
                        ? Mono.error(new ExcelReadException("缺少上传文件: " + name))
                        : Mono.just(parts));
    }

    private static List<FilePart> filePartsOf(MultiValueMap<String, Part> data, String name) {
        List<Part> parts = data.get(name);
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        List<FilePart> files = new ArrayList<>(parts.size());
        for (Part part : parts) {
            if (part instanceof FilePart filePart) {
                files.add(filePart);
            }
        }
        return files;
    }

    // ==================== 组装参数 ====================

    private Mono<Object> toArgument(List<FilePart> parts, Class<?> container,
                                    Class<?> rowType, ExcelImport annotation) {
        if (parts.isEmpty()) {
            return Mono.just(emptyValue(container));
        }
        List<FilePart> selected = annotation.merge() ? parts : List.of(parts.getFirst());
        if (reactiveSupport.supports(container)) {
            return Mono.just(reactiveSupport.streamOf(streamSources(selected, rowType, annotation)));
        }
        if (Flux.class.isAssignableFrom(container)) {
            return Mono.just(fluxOf(selected, rowType, annotation));
        }
        return readAll(selected, rowType, annotation)
                .map(result -> ExcelReadResult.class.isAssignableFrom(container) ? result : result.rows());
    }

    private Object emptyValue(Class<?> container) {
        if (reactiveSupport.supports(container)) {
            return reactiveSupport.emptyStream();
        }
        if (Flux.class.isAssignableFrom(container)) {
            return Flux.empty();
        }
        return ExcelReadResult.class.isAssignableFrom(container) ? ExcelReadResult.empty() : List.of();
    }

    /** 全量读：解析放在 boundedElastic 上，读完即删临时文件 */
    @SuppressWarnings("unchecked")
    private Mono<ExcelReadResult<Object>> readAll(List<FilePart> parts, Class<?> rowType, ExcelImport annotation) {
        return Flux.fromIterable(parts)
                .concatMap(part -> spill(part).flatMap(temp -> Mono
                        .fromCallable(() -> (ExcelReadResult<Object>) specFactory.readSpec(rowType, annotation)
                                .from(temp.toFile())
                                .execute())
                        .subscribeOn(Schedulers.boundedElastic())
                        .doFinally(signal -> ExcelUploadHelper.deleteQuietly(temp))))
                .reduce(ExcelReadResult.empty(), ExcelImportReactiveArgumentResolver::merge);
    }

    /** 流式读：每个文件一段懒执行的 Flux，订阅时落盘、终结时删文件 */
    private Flux<Object> fluxOf(List<FilePart> parts, Class<?> rowType, ExcelImport annotation) {
        return Flux.fromIterable(parts).concatMap(part -> Flux.usingWhen(
                spill(part),
                temp -> Flux.<Object>create(sink -> specFactory.readSpec(rowType, annotation)
                                .from(temp.toFile())
                                .consumeWhile(row -> {
                                    sink.next(row);
                                    return !sink.isCancelled();
                                })
                                .onFailure(sink::error)
                                .onSuccess(count -> sink.complete()))
                        .subscribeOn(Schedulers.boundedElastic()),
                temp -> Mono.fromRunnable(() -> ExcelUploadHelper.deleteQuietly(temp))));
    }

    private Mono<Path> spill(FilePart part) {
        String tempDir = properties.getWeb().getTempDir();
        return Mono.fromCallable(() -> ExcelUploadHelper.createTempFile(tempDir))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(temp -> part.transferTo(temp).thenReturn(temp));
    }

    private List<ExcelStreamSource> streamSources(List<FilePart> parts, Class<?> rowType, ExcelImport annotation) {
        String tempDir = properties.getWeb().getTempDir();
        List<ExcelStreamSource> sources = new ArrayList<>(parts.size());
        for (FilePart part : parts) {
            sources.add(new FilePartStreamSource(part, tempDir, rowType, annotation, specFactory));
        }
        return sources;
    }

    private static <T> ExcelReadResult<T> merge(ExcelReadResult<T> left, ExcelReadResult<T> right) {
        List<T> rows = new ArrayList<>(left.rows());
        rows.addAll(right.rows());
        List<ExcelRowError> errors = new ArrayList<>(left.errors());
        errors.addAll(right.errors());
        return new ExcelReadResult<>(rows, errors);
    }
}
