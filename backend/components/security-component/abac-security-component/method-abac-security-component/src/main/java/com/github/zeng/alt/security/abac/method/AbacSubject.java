package com.github.zeng.alt.security.abac.method;

import java.util.Map;

/**
 * 宿主解析出的可信主体快照：租户与标量主体属性。属性最终由 AbacContext 校验标量约束。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacSubject(String tenant, Map<String, Object> attributes) {
    public AbacSubject {
        attributes = Map.copyOf(attributes);
    }
}