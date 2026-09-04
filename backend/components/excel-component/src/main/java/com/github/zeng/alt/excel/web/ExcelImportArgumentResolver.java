package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.read.ExcelRowError;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把上传的 Excel 文件解析成 {@link ExcelImport} 标注的方法参数。
 * <p>
 * 支持 {@code List<T>}、{@link ExcelReadResult}{@code <T>} 与 {@link Flowable}{@code <T>}
 * 三种形状，元素类型从参数泛型推断。
 * <p>
 * 与旧实现的区别：不再继承 {@code AbstractMessageConverterMethodArgumentResolver}
 * （用不到消息转换器）、不再返回 {@code null}、不再在解析器里对 {@code Flowable}
 * 做 {@code blockingStream()}；{@code Flowable} 形状的上传内容先落临时文件，
 * 因此订阅时源仍然可用——旧实现拿的是请求结束即关闭的 multipart 流。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelImportArgumentResolver implements HandlerMethodArgumentResolver {

    private final ExcelTemplate excelTemplate;
    private final ExcelProperties properties;

    /**
     * 参数 → 行类型的缓存。
     * <p>
     * {@code ResolvableType.forMethodParameter} 要读泛型签名，是反射动作；
     * handler method 的数量是有限且固定的，缓存后每个参数只解析一次，
     * 而不是每次请求都解析。
     */
    private final Map<MethodParameter, Class<?>> rowTypeCache = new ConcurrentHashMap<>();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelImport.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        ExcelImport annotation = parameter.getParameterAnnotation(ExcelImport.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "参数 '%s' 上没有 @ExcelImport".formatted(parameter.getParameterName()));
        }
        Class<?> container = requireSupportedContainer(parameter);
        Class<?> rowType = rowTypeCache.computeIfAbsent(parameter, ExcelImportArgumentResolver::resolveRowType);
        List<MultipartFile> files = resolveFiles(parameter, webRequest, annotation);

        if (files.isEmpty()) {
            return emptyValue(container);
        }
        List<MultipartFile> selected = annotation.merge() ? files : List.of(files.getFirst());
        if (Flowable.class.isAssignableFrom(container)) {
            return concatStreams(selected, rowType, annotation);
        }
        ExcelReadResult<?> result = readAll(selected, rowType, annotation);
        return ExcelReadResult.class.isAssignableFrom(container) ? result : result.rows();
    }

    // ==================== 参数形状 ====================

    private static Class<?> requireSupportedContainer(MethodParameter parameter) {
        Class<?> container = parameter.getParameterType();
        if (isSupportedContainer(container)) {
            return container;
        }
        throw new IllegalArgumentException(
                "@ExcelImport 只支持 List<T> / ExcelReadResult<T> / Flowable<T>，参数 '%s' 是 %s"
                        .formatted(parameter.getParameterName(), container.getName()));
    }

    /**
     * 参数容器是否是本解析器支持的形状。
     *
     * @param container 参数类型
     * @return true 表示支持
     */
    public static boolean isSupportedContainer(Class<?> container) {
        return Collection.class.isAssignableFrom(container)
                || Flowable.class.isAssignableFrom(container)
                || ExcelReadResult.class.isAssignableFrom(container);
    }

    private static Class<?> resolveRowType(MethodParameter parameter) {
        Class<?> rowType = ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve();
        if (rowType == null) {
            throw new IllegalArgumentException("@ExcelImport 参数 '%s' 缺少泛型实参，无法确定行类型"
                    .formatted(parameter.getParameterName()));
        }
        return rowType;
    }

    private static Object emptyValue(Class<?> container) {
        if (Flowable.class.isAssignableFrom(container)) {
            return Flowable.empty();
        }
        return ExcelReadResult.class.isAssignableFrom(container) ? ExcelReadResult.empty() : List.of();
    }

    private static List<MultipartFile> resolveFiles(MethodParameter parameter, NativeWebRequest webRequest,
                                                    ExcelImport annotation) {
        String name = StringUtils.hasText(annotation.value()) ? annotation.value() : parameter.getParameterName();
        MultipartRequest request = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
        List<MultipartFile> files = request == null ? List.of() : request.getFiles(name);
        if (files.isEmpty() && annotation.required()) {
            throw new ExcelReadException("缺少上传文件: " + name);
        }
        return files;
    }

    // ==================== 读取 ====================

    private ExcelReadResult<?> readAll(List<MultipartFile> files, Class<?> rowType, ExcelImport annotation)
            throws Exception {
        List<Object> rows = new ArrayList<>();
        List<ExcelRowError> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            ExcelReadResult<?> result = readSpec(rowType, annotation).from(file.getInputStream()).execute();
            rows.addAll(result.rows());
            errors.addAll(result.errors());
        }
        return new ExcelReadResult<>(rows, errors);
    }

    private Flowable<?> concatStreams(List<MultipartFile> files, Class<?> rowType, ExcelImport annotation) {
        List<Flowable<?>> streams = new ArrayList<>(files.size());
        String tempDir = properties.getWeb().getTempDir();
        for (MultipartFile file : files) {
            streams.add(ExcelUploadHelper.spilledStream(file, tempDir,
                    temp -> readSpec(rowType, annotation).from(temp).stream()));
        }
        return Flowable.concat(streams);
    }

    @SuppressWarnings("unchecked")
    private ExcelReadSpec<?> readSpec(Class<?> rowType, ExcelImport annotation) {
        ExcelReadSpec<?> spec = annotation.dynamic()
                ? excelTemplate.readDynamic((Class<DynamicColumn<DynamicCell>>) rowType)
                : excelTemplate.read(rowType);
        spec.validate(annotation.validate())
                .skipInvalidRows(annotation.skipInvalidRows())
                .i18nHead(annotation.i18nHead());
        if (annotation.headRowNumber() >= 0) {
            spec.headRowNumber(annotation.headRowNumber());
        }
        return spec;
    }
}
