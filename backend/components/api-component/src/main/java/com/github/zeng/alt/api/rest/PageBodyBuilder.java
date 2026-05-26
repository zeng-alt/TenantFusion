package com.github.zeng.alt.api.rest;

import org.springframework.lang.Nullable;

import java.util.Collection;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2026年05月26日
 */
public interface PageBodyBuilder {

    <T> PageResponseEntity<T> body(@Nullable PageEntity<Collection<T>> body);

}
