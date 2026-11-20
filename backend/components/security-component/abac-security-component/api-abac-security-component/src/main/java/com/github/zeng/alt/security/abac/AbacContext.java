package com.github.zeng.alt.security.abac;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次授权的不可变数据快照，保留旧的主体、资源和环境入口。
 * resource.id 使用字符串表示业务对象 ID。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacContext {
    private final String tenant;
    private final String permission;
    private final Map<String, Object> principal;
    private final Map<String, Object> resource;
    private final Map<String, Object> env;
    private final AbacInvocationDto invocation;

    public AbacContext(String tenant, String permission, Map<String, Object> principal,
            Map<String, Object> resource, Map<String, Object> env) {
        Assert.hasText(tenant, "tenant must not be blank");
        Assert.hasText(permission, "permission must not be blank");
        this.tenant = tenant;
        this.permission = permission;
        this.principal = snapshot(principal);
        this.resource = snapshot(resource);
        this.env = snapshot(env);
        this.invocation = AbacInvocationDto.method(Map.of(), Map.of(), false);
    }

    private AbacContext(AbacContext original, AbacInvocationDto invocation) {
        this.tenant = original.tenant;
        this.permission = original.permission;
        this.principal = original.principal;
        this.resource = original.resource;
        this.env = original.env;
        this.invocation = invocation;
    }

    public String tenant() { return tenant; }
    public String permission() { return permission; }
    public Map<String, Object> principal() { return principal; }
    public Map<String, Object> resource() { return resource; }
    public Map<String, Object> env() { return env; }
    public AbacInvocationDto invocation() { return invocation; }

    public AbacContext withInvocation(AbacInvocationDto value) {
        return new AbacContext(this, value);
    }

    /** 深复制只读属性树，拒绝可调用的业务对象。 */
    public static Map<String, Object> snapshot(Map<String, Object> attributes) {
        Assert.notNull(attributes, "attributes must not be null");
        final Map<String, Object> values = new LinkedHashMap<>();
        attributes.forEach((key, value) -> {
            Assert.hasText(key, "attribute name must not be blank");
            values.put(key, snapshotValue(value));
        });
        return Collections.unmodifiableMap(values);
    }

    /** 仅接收 JSON 形状的数据，已声明可空字段保留 null。 */
    static Object snapshotValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double || value instanceof BigDecimal
                || value instanceof BigInteger) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> values = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                Assert.isInstanceOf(String.class, key, "attribute keys must be strings");
                values.put((String) key, item);
            });
            return snapshot(values);
        }
        if (value instanceof List<?> list) {
            final List<Object> values = new ArrayList<>();
            list.forEach(item -> values.add(snapshotValue(item)));
            return Collections.unmodifiableList(values);
        }
        throw new IllegalArgumentException("attributes must contain only scalar values, maps and lists");
    }
}
