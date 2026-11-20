package com.github.zeng.alt.security.core;

/**
 * 由宿主贡献两类 HTTP 白名单规则的窄粒度装配钩子。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface WhiteListCustomizer {
    void customize(WhiteListTemplate whiteList);
}
