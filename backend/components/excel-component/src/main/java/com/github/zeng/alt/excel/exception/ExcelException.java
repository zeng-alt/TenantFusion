package com.github.zeng.alt.excel.exception;

import com.github.zeng.alt.api.exception.BaseI18nException;

/**
 * Excel 组件异常基类。
 * <p>
 * 只用于「真正异常」的路径（文件损坏、IO 失败、写出中断）。业务上预期内的失败
 * （某一行校验不通过、表头缺列）不抛异常，走 {@code Either} /
 * {@link com.github.zeng.alt.excel.read.ExcelReadResult}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelException extends BaseI18nException {

    public ExcelException(String message) {
        super(message);
    }

    /**
     * 带根因构造。
     * <p>
     * 不能写成 {@code super(message, cause)}——{@code BaseException(String, Throwable)}
     * 的第一个形参是 {@code title} 而非 {@code message}，会把消息挪到标题上。
     *
     * @param message 异常消息，支持 {@code {i18n.key}} 形式
     * @param cause   根因
     */
    public ExcelException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
