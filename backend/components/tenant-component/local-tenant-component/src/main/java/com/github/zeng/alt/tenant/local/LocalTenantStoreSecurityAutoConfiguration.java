package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 本地租户库安全自动配置：服务密钥通道。
 * <p>
 * 仅当配了 {@code alt.tenant.store.service-key} 且容器里有 Spring Security
 * 时生效（无安全框架的普通 Web 应用不受影响）。注册两样东西：
 * <ol>
 *   <li>{@code SecurityBuilderCustomizer}：把 {@link TenantServiceKeyFilter}
 *       挂进安全链（与 JWT 过滤器同一位置之前），命中则放入服务身份。
 *       必须在链内——链外的 Servlet 过滤器设置的身份会被链内的
 *       {@code SecurityContextHolderFilter} 覆盖。</li>
 *   <li>{@link TenantServiceAuthorizationManager}：只给“服务身份 × 元数据路径”放行</li>
 * </ol>
 * 用户 JWT 链路完全不受影响（有效用户 token 会覆盖服务身份）；
 * service-key 拿不到超管权限，打不开其它接口。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@AutoConfiguration(after = LocalTenantStoreWebAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "alt.tenant.store", name = {"url", "service-key"})
@ConditionalOnClass(name = {
        "org.springframework.security.authorization.AuthorizationManager",
        "org.springframework.security.web.access.intercept.RequestAuthorizationContext",
        "com.github.zeng.alt.security.api.AuthorizationManagerProvider",
        "com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer"})
public class LocalTenantStoreSecurityAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "tenantServiceKeySecurityCustomizer")
    public SecurityBuilderCustomizer tenantServiceKeySecurityCustomizer(
            TenantStoreProperties properties) {
        return http -> http.addFilterBefore(
                new TenantServiceKeyFilter(properties),
                UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    @ConditionalOnMissingBean(TenantServiceAuthorizationManager.class)
    public TenantServiceAuthorizationManager tenantServiceAuthorizationManager(
            TenantStoreProperties properties) {
        return new TenantServiceAuthorizationManager(properties);
    }
}
