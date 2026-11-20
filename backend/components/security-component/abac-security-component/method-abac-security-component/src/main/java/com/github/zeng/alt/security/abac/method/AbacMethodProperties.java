package com.github.zeng.alt.security.abac.method;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * 方法级 ABAC 配置。enabled 默认开启；ABAC 位于事务内部，后置拒绝可回滚方法写入。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.abac.method")
public class AbacMethodProperties {
    private boolean enabled = true;
    private int order = Ordered.LOWEST_PRECEDENCE;
}