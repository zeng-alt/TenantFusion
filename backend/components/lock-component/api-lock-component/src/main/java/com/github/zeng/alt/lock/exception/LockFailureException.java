package com.github.zeng.alt.lock.exception;

/**
 * 获取锁失败异常
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class LockFailureException extends LockException {

    public LockFailureException() {
    }

    public LockFailureException(String message) {
        super(message);
    }

    public LockFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
