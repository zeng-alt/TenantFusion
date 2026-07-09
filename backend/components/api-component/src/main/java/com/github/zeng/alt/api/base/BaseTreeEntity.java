package com.github.zeng.alt.api.base;

import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月08日
 * @version 1.0
 */
public interface BaseTreeEntity<T> {

    public T getParent();

    public List<T> getChildren();
}

