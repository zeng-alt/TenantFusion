package com.github.zeng.alt.api.rest;

import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

import java.util.Collection;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2026年05月26日
 */
public class PageBuilder implements PageBodyBuilder {

    private final HttpStatusCode statusCode;

    public PageBuilder(HttpStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public PageBuilder(Integer statusCode) {
        this.statusCode = HttpStatusCode.valueOf(statusCode);
    }

    @Override
    public <T> PageResponseEntity<T> body(@Nullable PageEntity<Collection<T>> body) {
        return new PageResponseEntity<>(body, statusCode);
    }
}
