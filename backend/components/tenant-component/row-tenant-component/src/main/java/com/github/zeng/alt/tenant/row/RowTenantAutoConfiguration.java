package com.github.zeng.alt.tenant.row;

import com.github.zeng.alt.tenant.api.TenantDiscriminatorPolicy;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.github.zeng.alt.tenant.core.TenantProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * 行级隔离自动配置。
 * <p>
 * 本模块是四个策略模块里最薄的一个：{@code @TenantId} 是 Hibernate 原生注解、标在实体上即可，
 * 而租户标识解析器所有档位都要用、只能放在 core。所以这里只装两样东西——
 * 判别列基类 {@link TenantBaseEntity} 与 {@link DefaultTenantDiscriminatorPolicy}。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(after = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
public class RowTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDiscriminatorPolicy.class)
    public DefaultTenantDiscriminatorPolicy defaultTenantDiscriminatorPolicy(
            TenantProperties properties, TenantRoutingRegistry registry) {
        return new DefaultTenantDiscriminatorPolicy(properties, registry);
    }
}
