package com.github.zeng.alt.excel.exception;

/**
 * Excel 写出异常：输出流不可用、模板缺失、写出过程中断等。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelWriteException extends ExcelException {

    public ExcelWriteException(String message) {
        super(message);
    }

    public ExcelWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
