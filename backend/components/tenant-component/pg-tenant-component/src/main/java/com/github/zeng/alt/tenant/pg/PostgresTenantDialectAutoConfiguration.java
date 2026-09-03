package com.github.zeng.alt.tenant.pg;

import com.github.zeng.alt.tenant.api.TenantDialect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * PostgreSQL 方言自动配置。
 * <p>
 * 与 H2 方言的先后顺序：两者都用
 * {@code @ConditionalOnMissingBean(TenantDialect.class)}，同时存在两个驱动时结果取决于
 * 自动配置顺序。生产库为 PG、H2 仅测试用，因此显式声明本类优先——
 * 需要强制指定时直接自行注册一个 {@code TenantDialect} Bean 即可覆盖。
 * 用 {@code beforeName} 的字符串形式而非 class 引用，是为了不让 pg 模块编译期依赖 h2 模块。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(beforeName = "com.github.zeng.alt.tenant.h2.H2TenantDialectAutoConfiguration")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "org.postgresql.Driver")
public class PostgresTenantDialectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDialect.class)
    public PostgresTenantDialect postgresTenantDialect() {
        return new PostgresTenantDialect();
    }
}
