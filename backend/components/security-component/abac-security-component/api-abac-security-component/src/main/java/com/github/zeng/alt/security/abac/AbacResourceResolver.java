package com.github.zeng.alt.security.abac;

import java.util.Map;

/**
 * 资源解析 SPI（宿主实现并注册为 Spring bean，在 {@link AbacAuthorize#resources()} 显式声明）。
 * 从方法上下文贡献标量资源属性，多个解析器结果按声明顺序合并（后者覆盖同名键）。
 * 资源 ID 必须以 "id" 键贡献，供按资源实例绑定的策略匹配。不得返回 null。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacResourceResolver {
    Map<String, Object> resolve(AbacMethodContext context);
}