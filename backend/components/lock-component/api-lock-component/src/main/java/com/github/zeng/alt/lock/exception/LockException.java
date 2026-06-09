package com.github.zeng.alt.lock.exception;

import com.github.zeng.alt.api.exception.UtilException;

/**
 * 分布式锁基础异常
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class LockException extends UtilException {

    public LockException() {
        super();
    }

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockException(Throwable cause) {
        super(cause);
    }
}
