package com.github.zeng.alt.security.abac;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * 单次授权的可信属性快照；属性只接受标量，不向表达式暴露实体、凭据或可调用对象。
 * resource.id 使用字符串表示业务对象 ID，environment.action 表示业务动作。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacContext(
        String tenant,
        String permission,
        Map<String, Object> subject,
        Map<String, Object> resource,
        Map<String, Object> environment
) {
    public AbacContext {
        Assert.hasText(tenant, "tenant must not be blank");
        Assert.hasText(permission, "permission must not be blank");
        subject = snapshot(subject);
        resource = snapshot(resource);
        environment = snapshot(environment);
    }

    private static Map<String, Object> snapshot(Map<String, Object> attributes) {
        Assert.notNull(attributes, "attributes must not be null");
        attributes.forEach((key, value) -> {
            Assert.hasText(key, "attribute name must not be blank");
            Assert.isTrue(value instanceof String || value instanceof Boolean || value instanceof Byte
                    || value instanceof Short || value instanceof Integer || value instanceof Long
                    || value instanceof Float || value instanceof Double || value instanceof BigDecimal
                    || value instanceof BigInteger, "attributes must be non-null scalar values");
        });
        return Map.copyOf(attributes);
    }
}
