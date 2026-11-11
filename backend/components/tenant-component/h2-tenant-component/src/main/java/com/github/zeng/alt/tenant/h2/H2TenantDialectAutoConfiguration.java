package com.github.zeng.alt.tenant.h2;

import com.github.zeng.alt.tenant.api.TenantDialect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * H2 方言自动配置：按驱动是否在 classpath 上自动挑选，使用方无需手动指定。
 * <p>
 * 增加一层按业务库驱动名的选择（{@code spring.datasource.driver-class-name}），
 * 解决双驱动共存时的误选：admin 同时带 h2（dev）与 pg（prod）驱动，
 * 旧逻辑下 PG 方言恒胜，在 H2 的 dev 库上执行 PG 的 schema 语句会直接失败。
 * 未配置驱动名时回退到旧行为（PG 优先）。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "org.h2.Driver")
@ConditionalOnProperty(prefix = "spring.datasource", name = "driver-class-name",
        havingValue = "org.h2.Driver")
public class H2TenantDialectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDialect.class)
    public H2TenantDialect h2TenantDialect() {
        return new H2TenantDialect();
    }
}
