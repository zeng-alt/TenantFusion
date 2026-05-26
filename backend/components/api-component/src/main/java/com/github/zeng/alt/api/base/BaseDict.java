package com.github.zeng.alt.api.base;

import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年05月26日
 * @version 1.0
 */
public interface BaseDict<T> {


    String getLabel();

    T getValue();

    default boolean getDisabled() {
        return false;
    }

    default List<BaseDict<T>> getChildren() {
        return List.of();
    }
}
