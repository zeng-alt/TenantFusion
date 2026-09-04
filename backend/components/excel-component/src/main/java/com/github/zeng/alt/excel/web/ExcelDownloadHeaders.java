package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.support.ExcelFileNameHelper;
import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

/**
 * 下载响应头的取值，与具体 Web 栈无关。
 * <p>
 * Servlet 栈往 {@code HttpServletResponse} 上写，WebFlux 栈往
 * {@code ServerHttpResponse#getHeaders()} 上写，头的内容是同一套，所以算在这里。
 *
 * @param contentType        {@code Content-Type}
 * @param contentDisposition {@code Content-Disposition}
 * @param fileName           百分号编码后的文件名，另外放在 {@code download-filename} 里
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelDownloadHeaders(String contentType, String contentDisposition, String fileName) {

    /** xlsx 的 MIME 类型 */
    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** 需要暴露给浏览器脚本的响应头名 */
    public static final String EXPOSE_HEADERS = "Content-Disposition,download-filename";

    /** 承载文件名的自定义头，便于前端不解析 Content-Disposition 也能拿到 */
    public static final String FILE_NAME_HEADER = "download-filename";

    /**
     * 按 {@code @ExcelExport} 与方法名算出下载头。
     *
     * @param annotation 导出注解
     * @param returnType 返回值，用于取方法名兜底文件名
     * @return 下载头
     */
    public static ExcelDownloadHeaders of(ExcelExport annotation, MethodParameter returnType) {
        return of(annotation, returnType, ExcelFileNameHelper.DEFAULT_TIMESTAMP_PATTERN);
    }

    /**
     * 按 {@code @ExcelExport} 与方法名算出下载头，时间戳用指定格式。
     *
     * @param annotation       导出注解
     * @param returnType       返回值，用于取方法名兜底文件名
     * @param timestampPattern 时间戳格式，取自 {@code alt.excel.write.file-name-timestamp-pattern}
     * @return 下载头
     */
    public static ExcelDownloadHeaders of(
            ExcelExport annotation, MethodParameter returnType, String timestampPattern) {
        String base = StringUtils.hasText(annotation.fileName()) ? annotation.fileName() : annotation.value();
        if (!StringUtils.hasText(base)) {
            base = returnType.getMethod() == null ? "export" : returnType.getMethod().getName();
        }
        String fileName = ExcelFileNameHelper.build(
                ExcelMessageHelper.resolve(base), annotation.timestamp(), timestampPattern);
        String encoded = ExcelFileNameHelper.percentEncode(fileName);
        return new ExcelDownloadHeaders(
                XLSX_CONTENT_TYPE + ";charset=UTF-8",
                "attachment; filename=%s; filename*=utf-8''%s".formatted(encoded, encoded),
                encoded);
    }
}
