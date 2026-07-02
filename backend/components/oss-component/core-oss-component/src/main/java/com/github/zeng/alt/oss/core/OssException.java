package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.api.exception.BaseException;

import java.io.Serial;

/**
 * OSS 操作异常。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class OssException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
    }
}
