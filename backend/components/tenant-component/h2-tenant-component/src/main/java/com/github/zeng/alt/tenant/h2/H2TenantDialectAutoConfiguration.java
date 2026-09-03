package com.github.zeng.alt.tenant.h2;

import com.github.zeng.alt.tenant.api.TenantDialect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * H2 方言自动配置：按驱动是否在 classpath 上自动挑选，使用方无需手动指定。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "org.h2.Driver")
public class H2TenantDialectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDialect.class)
    public H2TenantDialect h2TenantDialect() {
        return new H2TenantDialect();
    }
}
