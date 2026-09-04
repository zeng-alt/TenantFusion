package com.github.zeng.alt.excel.web.servlet;

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

/**
 * Servlet 栈：把上传的 Excel 文件解析成 {@link ExcelImport} 标注的方法参数。
 * <p>
 * 支持 {@code List<T>}、{@link ExcelReadResult}{@code <T>} 与 {@code Flowable<T>}
 * 三种形状，元素类型从参数泛型推断。{@code Flowable} 形状经
 * {@link ExcelReactiveSupport} 适配——RxJava 是可选依赖，本类不引用它的任何类型。
 * <p>
 * WebFlux 应用请看
 * {@code com.github.zeng.alt.excel.web.reactive.ExcelImportReactiveArgumentResolver}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelImportArgumentResolver implements HandlerMethodArgumentResolver {

    private final ExcelWebSpecFactory specFactory;
    private final ExcelProperties properties;
    private final ExcelReactiveSupport reactiveSupport;
    private final ExcelRowTypeResolver rowTypeResolver = new ExcelRowTypeResolver();

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
        Class<?> rowType = rowTypeResolver.resolve(parameter, "@ExcelImport");
        List<MultipartFile> files = resolveFiles(parameter, webRequest, annotation);

        if (files.isEmpty()) {
            return emptyValue(container);
        }
        List<MultipartFile> selected = annotation.merge() ? files : List.of(files.getFirst());
        if (reactiveSupport.supports(container)) {
            return reactiveSupport.streamOf(streamSources(selected, rowType, annotation));
        }
        ExcelReadResult<?> result = readAll(selected, rowType, annotation);
        return ExcelReadResult.class.isAssignableFrom(container) ? result : result.rows();
    }

    // ==================== 参数形状 ====================

    private Class<?> requireSupportedContainer(MethodParameter parameter) {
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
    public boolean isSupportedContainer(Class<?> container) {
        return Collection.class.isAssignableFrom(container)
                || ExcelReadResult.class.isAssignableFrom(container)
                || reactiveSupport.supports(container);
    }

    private Object emptyValue(Class<?> container) {
        if (reactiveSupport.supports(container)) {
            return reactiveSupport.emptyStream();
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
            ExcelReadResult<?> result = specFactory.readSpec(rowType, annotation)
                    .from(file.getInputStream())
                    .execute();
            rows.addAll(result.rows());
            errors.addAll(result.errors());
        }
        return new ExcelReadResult<>(rows, errors);
    }

    /**
     * 每个上传文件包成一个懒打开的来源：订阅时才落盘，流终结时删文件。
     * <p>
     * 必须落盘——multipart 的原始存储在请求结束时就被 servlet 容器回收了，
     * 而响应式流是懒执行的，订阅时再去读原始流必然失败。
     */
    private List<ExcelStreamSource> streamSources(List<MultipartFile> files, Class<?> rowType,
                                                  ExcelImport annotation) {
        String tempDir = properties.getWeb().getTempDir();
        List<ExcelStreamSource> sources = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            sources.add(new MultipartStreamSource(file, tempDir, rowType, annotation, specFactory));
        }
        return sources;
    }
}
