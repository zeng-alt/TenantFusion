package com.github.zeng.alt.api.rest;

import org.springframework.lang.Nullable;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月13日 21:43
 */
public class HttpEntityStatus<T> extends HttpEntity<T> {

    protected final Integer code;

    public HttpEntityStatus(@Nullable T body, Integer status) {
        super(body);
        this.code = status;
    }

    /**
     * Return the HTTP status code of the response.
     * @return the HTTP status as an HttpStatus enum entry
     */
    public Integer getStatus() {
        return this.code;
    }
}
