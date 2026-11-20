package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacResourceResolver;

import java.util.List;

/**
 * 注解解析后的授权意图快照，按 MethodClassKey 缓存。permission 为权限码，
 * requirePolicy 控制无适用策略时是否拒绝，resources 为声明的资源解析器类（按声明顺序）。
 *
 * @param permission 权限码
 * @param requirePolicy 是否要求存在策略
 * @param resources 声明的资源解析器
 * @param metadata 缓存的方法、参数与返回类型元数据
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacAuthorizeAttribute(
        String permission,
        boolean requirePolicy,
        List<Class<? extends AbacResourceResolver>> resources,
        AbacMethodDto metadata,
        boolean transactional
) {


    public AbacAuthorizeAttribute {
        resources = List.copyOf(resources);
    }
}
