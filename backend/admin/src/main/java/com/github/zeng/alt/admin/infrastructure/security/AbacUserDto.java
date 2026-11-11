package com.github.zeng.alt.admin.infrastructure.security;

import io.vavr.control.Option;

import java.util.Map;

/**
 * 授权用用户快照，不包含口令、联系方式或关联实体。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacUserDto(String id, String tenant, Option<Long> deptId, boolean enabled) {
    public Map<String, Object> attributes() {
        final var attributes = io.vavr.collection.HashMap.<String, Object>of(
                "id", id, "tenant", tenant, "enabled", enabled
        );
        return Map.copyOf(deptId.map(value -> attributes.put("deptId", value)).getOrElse(attributes).toJavaMap());
    }
}
