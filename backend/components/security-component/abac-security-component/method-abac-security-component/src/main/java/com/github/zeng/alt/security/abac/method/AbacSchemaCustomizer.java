package com.github.zeng.alt.security.abac.method;

/**
 * 宿主贡献主体和环境的字段描述，与真实属性提供者保持一致。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacSchemaCustomizer {
    void customize(AbacSchemaBuilder schema);
}
