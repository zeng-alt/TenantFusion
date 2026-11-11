package com.github.zeng.alt.tenant.pg;

import com.github.zeng.alt.tenant.api.TenantDialect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>
 * 另按业务库驱动名做一层选择：配了 {@code spring.datasource.driver-class-name=org.h2.Driver}
 * 的应用（dev）只会激活 H2 方言；未配驱动名时回退到旧行为（本类优先）。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(beforeName = "com.github.zeng.alt.tenant.h2.H2TenantDialectAutoConfiguration")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "org.postgresql.Driver")
@ConditionalOnProperty(prefix = "spring.datasource", name = "driver-class-name",
        havingValue = "org.postgresql.Driver", matchIfMissing = true)
public class PostgresTenantDialectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDialect.class)
    public PostgresTenantDialect postgresTenantDialect() {
        return new PostgresTenantDialect();
    }
}
