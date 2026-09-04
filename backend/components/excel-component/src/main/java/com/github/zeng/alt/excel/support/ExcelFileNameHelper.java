package com.github.zeng.alt.excel.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 下载文件名的组装与编码，内部便利类。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public final class ExcelFileNameHelper {

    /** xlsx 扩展名 */
    public static final String XLSX_SUFFIX = ".xlsx";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ExcelFileNameHelper() {
    }

    /**
     * 组装下载文件名。
     *
     * @param baseName  文件名主体，为空时用 {@code export}
     * @param timestamp 是否追加时间戳
     * @return 含 {@code .xlsx} 扩展名的文件名
     */
    public static String build(String baseName, boolean timestamp) {
        String name = baseName == null || baseName.isBlank() ? "export" : baseName.strip();
        if (name.endsWith(XLSX_SUFFIX)) {
            name = name.substring(0, name.length() - XLSX_SUFFIX.length());
        }
        String suffix = timestamp ? "_" + LocalDateTime.now().format(TIMESTAMP) : "";
        return name + suffix + XLSX_SUFFIX;
    }

    /**
     * 百分号编码，供 {@code Content-Disposition} 使用。
     * <p>
     * {@code URLEncoder} 会把空格编成 {@code +}，而 HTTP 头里需要 {@code %20}。
     *
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String percentEncode(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
