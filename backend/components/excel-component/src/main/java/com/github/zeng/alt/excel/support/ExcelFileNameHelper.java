package com.github.zeng.alt.excel.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 时间戳格式非法时的兜底 */
    public static final String DEFAULT_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    private static final Log LOG = LogFactory.getLog(ExcelFileNameHelper.class);

    /**
     * 格式串 → 解析好的 formatter。
     * <p>
     * 格式串来自配置、取值固定，没必要每次导出都重新 parse；顺带让「格式非法」
     * 的 warn 只打一次而不是每次下载都刷一条。
     */
    private static final Map<String, DateTimeFormatter> FORMATTERS = new ConcurrentHashMap<>();

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
        return build(baseName, timestamp, DEFAULT_TIMESTAMP_PATTERN);
    }

    /**
     * 组装下载文件名，时间戳用指定格式。
     *
     * @param baseName  文件名主体，为空时用 {@code export}
     * @param timestamp 是否追加时间戳
     * @param pattern   时间戳格式，非法或为空时退回 {@link #DEFAULT_TIMESTAMP_PATTERN}
     * @return 含 {@code .xlsx} 扩展名的文件名
     */
    public static String build(String baseName, boolean timestamp, String pattern) {
        String name = baseName == null || baseName.isBlank() ? "export" : baseName.strip();
        if (name.endsWith(XLSX_SUFFIX)) {
            name = name.substring(0, name.length() - XLSX_SUFFIX.length());
        }
        String suffix = timestamp ? "_" + LocalDateTime.now().format(formatter(pattern)) : "";
        return name + suffix + XLSX_SUFFIX;
    }

    /**
     * 格式非法时记 warn 后退回默认值，不让一个配置笔误把导出整个搞挂。
     */
    private static DateTimeFormatter formatter(String pattern) {
        String key = pattern == null || pattern.isBlank() ? DEFAULT_TIMESTAMP_PATTERN : pattern;
        return FORMATTERS.computeIfAbsent(key, ExcelFileNameHelper::parse);
    }

    private static DateTimeFormatter parse(String pattern) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
            LOG.warn("时间戳格式非法，退回 " + DEFAULT_TIMESTAMP_PATTERN + ": " + pattern);
            return DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_PATTERN);
        }
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
