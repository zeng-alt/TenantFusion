package com.github.zeng.alt.api.exception;

import lombok.Getter;

/**
 * @author zengJiaJun
 * @since  2026年04月21日
 * @version 1.0
 */
@Getter
public class BaseException extends RuntimeException {

    private String title;
    private Integer code = 500;

    public BaseException() {}

    public BaseException(Throwable throwable) {
        super(throwable);
    }

    public BaseException(Integer code, Throwable throwable) {
        super(throwable);
        this.code = code;
    }

    public BaseException(String title, Throwable throwable) {
        super(throwable);
        this.title = title;
    }

    public BaseException(String message) {
        super(message);
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(String title, String message) {
        super(message);
        this.title = title;
    }

    public BaseException(Integer code, String title, String message) {
        super(message);
        this.code = code;
        this.title = title;
    }


}
