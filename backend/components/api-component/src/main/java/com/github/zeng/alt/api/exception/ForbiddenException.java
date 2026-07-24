package com.github.zeng.alt.api.exception;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(403, "权限不足!!!", message);
    }

    public ForbiddenException() {
        super(403, "权限不足!!!");
    }
}
