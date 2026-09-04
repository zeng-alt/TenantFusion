package com.github.zeng.alt.excel.web.servlet;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.web.ExcelDownloadHeaders;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelRowTypeResolver;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Try;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Servlet 栈：把 {@link ExcelExport} 标注的方法返回值写成 Excel 附件下发。
 * <p>
 * 旧版本只有注解、没有任何处理器，导出功能实际上不存在；本类补上这一半。
 * <p>
 * 注册时必须插到内置处理器「之前」：{@code List<T>} 这类返回值会被
 * {@code RequestResponseBodyMethodProcessor} 先接走，靠
 * {@code WebMvcConfigurer#addReturnValueHandlers} 追加是拿不到的，
 * 见 {@code ExcelWebMvcAutoConfiguration}。
 * <p>
 * WebFlux 应用请看
 * {@code com.github.zeng.alt.excel.web.reactive.ExcelExportResultHandler}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelExportReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final ExcelWebSpecFactory specFactory;
    private final ExcelReactiveSupport reactiveSupport;
    private final ExcelRowTypeResolver rowTypeResolver = new ExcelRowTypeResolver();

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasMethodAnnotation(ExcelExport.class);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        mavContainer.setRequestHandled(true);
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        if (annotation == null) {
            throw new ExcelWriteException("返回值上找不到 @ExcelExport");
        }
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            throw new ExcelWriteException("当前请求不是 Servlet 请求，无法导出 Excel");
        }
        Class<?> rowType = resolveRowType(annotation, returnType);
        applyHeaders(response, ExcelDownloadHeaders.of(annotation, returnType));
        write(returnValue, rowType, annotation, response.getOutputStream())
                .getOrElseThrow(cause -> new ExcelWriteException("Excel 导出失败", cause));
    }

    private Try<Long> write(Object returnValue, Class<?> rowType, ExcelExport annotation, OutputStream output) {
        ExcelWriteSpec<Object> spec = specFactory.writeSpec(rowType, annotation);
        spec.to(output).autoCloseStream(false);

        if (returnValue == null) {
            return spec.write(List.of());
        }
        if (reactiveSupport.supports(returnValue.getClass())) {
            return reactiveSupport.iterator(returnValue).flatMap(spec::write);
        }
        if (returnValue instanceof Collection<?> collection) {
            return spec.write(unchecked(collection));
        }
        if (returnValue instanceof Iterator<?> iterator) {
            return spec.write(unchecked(iterator));
        }
        return Try.failure(new ExcelWriteException(
                "@ExcelExport 只支持 Collection<T> / Iterator<T> / Flowable<T> 返回值，实际是 "
                        + returnValue.getClass().getName()));
    }

    private static void applyHeaders(HttpServletResponse response, ExcelDownloadHeaders headers) {
        response.setContentType(headers.contentType());
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", headers.contentDisposition());
        response.addHeader("Access-Control-Expose-Headers", ExcelDownloadHeaders.EXPOSE_HEADERS);
        response.setHeader(ExcelDownloadHeaders.FILE_NAME_HEADER, headers.fileName());
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

    @SuppressWarnings("unchecked")
    private static Collection<Object> unchecked(Collection<?> collection) {
        return (Collection<Object>) collection;
    }

    @SuppressWarnings("unchecked")
    private static Iterator<Object> unchecked(Iterator<?> iterator) {
        return (Iterator<Object>) iterator;
    }
}
