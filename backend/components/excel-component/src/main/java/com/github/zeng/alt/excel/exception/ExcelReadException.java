package com.github.zeng.alt.excel.exception;

/**
 * Excel 读取异常：文件无法解析、密码错误、表头结构不符等整份文件级别的失败。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelReadException extends ExcelException {

    public ExcelReadException(String message) {
        super(message);
    }

    public ExcelReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
