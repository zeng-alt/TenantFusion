package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacResourceResolver;

import java.util.List;

/**
 * 注解解析后的授权意图快照，按 MethodClassKey 缓存。permission 为权限码，
 * requirePolicy 控制无适用策略时是否拒绝，resources 为声明的资源解析器类（按声明顺序）。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacAuthorizeAttribute(
        String permission,
        boolean requirePolicy,
        List<Class<? extends AbacResourceResolver>> resources
) {
    public AbacAuthorizeAttribute {
        resources = List.copyOf(resources);
    }
}