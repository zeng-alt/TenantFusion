package com.github.zeng.alt.security.abac.client;

import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;

/**
 * 客户端缓存配置的代码贡献入口，不替换组件拥有的授权门面。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacClientCustomizer {
    void customize(AbacClientProperties properties);
}
