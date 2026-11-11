package com.github.zeng.alt.security.abac.method;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;

/**
 * 方法级 ABAC 配置。enabled 默认开启；order 默认与 Spring Security PRE_AUTHORIZE 同档。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.abac.method")
public class AbacMethodProperties {
    private boolean enabled = true;
    private int order = AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder();
}