package com.github.zeng.alt.api.base;

/**
 * @author zengJiaJun
 * @since 2026年05月26日
 * @version 1.0
 */
public interface BaseMenu {

    public String getKey();

    public String getLabel();

    default boolean getShow() {
        return true;
    }


}
