package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.support.ExcelFileNameHelper;
import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * 把 {@link ExcelExport} 标注的方法返回值写成 Excel 附件下发。
 * <p>
 * 旧版本只有注解、没有任何处理器，导出功能实际上不存在；本类补上这一半。
 * <p>
 * 注册时必须插到内置处理器「之前」：{@code List<T>} 这类返回值会被
 * {@code RequestResponseBodyMethodProcessor} 先接走，靠
 * {@code WebMvcConfigurer#addReturnValueHandlers} 追加是拿不到的，
 * 见 {@code ExcelWebAutoConfiguration}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelExportReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelTemplate excelTemplate;

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
        applyHeaders(response, annotation, returnType);
        write(returnValue, rowType, annotation, response.getOutputStream())
                .getOrElseThrow(cause -> new ExcelWriteException("Excel 导出失败", cause));
    }

    // ==================== 写出 ====================

    @SuppressWarnings("unchecked")
    private Try<Long> write(Object returnValue, Class<?> rowType, ExcelExport annotation, OutputStream output) {
        ExcelWriteSpec<Object> spec = (ExcelWriteSpec<Object>) excelTemplate.write(rowType);
        spec.to(output).autoCloseStream(false);
        if (StringUtils.hasText(annotation.sheetName())) {
            spec.sheet(ExcelMessageHelper.resolve(annotation.sheetName()));
        }
        // 注解上是三态开关：-1 沿用配置默认值，0/1 显式覆盖
        if (annotation.autoWidth() >= 0) {
            spec.autoWidth(annotation.autoWidth() == 1);
        }
        if (annotation.i18nHead() >= 0) {
            spec.i18nHead(annotation.i18nHead() == 1);
        }

        if (returnValue instanceof Flowable<?> flowable) {
            return spec.write((Flowable<Object>) flowable);
        }
        if (returnValue instanceof Collection<?> collection) {
            return spec.write((Collection<Object>) collection);
        }
        if (returnValue == null) {
            return spec.write(List.of());
        }
        return Try.failure(new ExcelWriteException(
                "@ExcelExport 只支持 Collection<T> 或 Flowable<T> 返回值，实际是 "
                        + returnValue.getClass().getName()));
    }

    // ==================== 响应头 ====================

    private void applyHeaders(HttpServletResponse response, ExcelExport annotation, MethodParameter returnType) {
        String base = StringUtils.hasText(annotation.fileName()) ? annotation.fileName() : annotation.value();
        if (!StringUtils.hasText(base)) {
            base = returnType.getMethod() == null ? "export" : returnType.getMethod().getName();
        }
        String fileName = ExcelFileNameHelper.build(ExcelMessageHelper.resolve(base), annotation.timestamp());
        String encoded = ExcelFileNameHelper.percentEncode(fileName);
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=%s; filename*=utf-8''%s".formatted(encoded, encoded));
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        response.setHeader("download-filename", encoded);
    }

    private static Class<?> resolveRowType(ExcelExport annotation, MethodParameter returnType) {
        if (annotation.type() != Object.class) {
            return annotation.type();
        }
        Class<?> rowType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
        if (rowType == null) {
            throw new ExcelWriteException(
                    "无法从返回值推断导出实体类型，请显式指定 @ExcelExport(type = Xxx.class)");
        }
        return rowType;
    }
}
