package com.github.zeng.alt.security.abac.server.config;

import lombok.Data;
import lombok.ToString;

import java.util.Set;

/**
 * 内部客户端身份及其明确允许读取的租户。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
public class AbacServiceClientProperties {
    @ToString.Exclude
    private String key = "";
    private Set<String> tenants = Set.of();
}
