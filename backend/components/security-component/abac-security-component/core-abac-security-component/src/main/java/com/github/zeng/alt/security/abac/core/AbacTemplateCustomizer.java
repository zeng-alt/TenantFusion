package com.github.zeng.alt.security.abac.core;

/**
 * 贡献组件配置；声明式配置无法表达的权限集合可在此补充，不需要替换门面 Bean。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacTemplateCustomizer {
    void customize(AbacProperties properties);
}
