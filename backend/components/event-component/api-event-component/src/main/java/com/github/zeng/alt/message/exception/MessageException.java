package com.github.zeng.alt.message.exception;

import java.io.Serial;

/**
 * 消息模块基础异常。
 * <p>
 * Base exception for the message module.
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class MessageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
