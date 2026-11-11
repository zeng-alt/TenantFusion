package com.github.zeng.alt.security.abac.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * ABAC 配置。必需权限没有适用策略时拒绝；未列出的权限存在策略时仍会执行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.abac")
public class AbacProperties {
    private Set<String> requiredPermissions = Set.of();
}
