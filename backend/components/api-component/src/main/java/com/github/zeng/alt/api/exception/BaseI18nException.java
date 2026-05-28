package com.github.zeng.alt.api.exception;

/**
 * @author zengJiaJun
 * @since 2026年04月21日
 * @version 1.0
 */
public class BaseI18nException extends BaseException {

    public BaseI18nException() {}

    public BaseI18nException(Throwable throwable) {
        super(throwable);
    }

    public BaseI18nException(Integer code, Throwable throwable) {
        super(code, throwable);
    }

    public BaseI18nException(String title, Throwable throwable) {
        super(title, throwable);
    }

    public BaseI18nException(String message) {
        super(message);
    }

    public BaseI18nException(Integer code, String message) {
        super(code, message);
    }

    public BaseI18nException(String title, String message) {
        super(title, message);
    }

    public BaseI18nException(Integer code, String title, String message) {
        super(code, title, message);
    }
}
