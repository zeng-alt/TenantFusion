package com.github.zeng.alt.api.exception;

/**
 * @author zengJiaJun
 * @since 2026年04月21日
 * @version 1.0
 */
public class UtilException extends BaseException {

    public UtilException() {}

    public UtilException(Throwable throwable) {
        super(throwable);
    }

    public UtilException(String message) {
        super(message);
    }
}
