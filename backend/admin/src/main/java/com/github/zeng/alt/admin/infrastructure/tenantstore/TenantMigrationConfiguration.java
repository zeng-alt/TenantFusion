package com.github.zeng.alt.admin.infrastructure.tenantstore;

import com.github.zeng.alt.tenant.api.TenantRoutingException;
import com.github.zeng.alt.tenant.database.TenantDataSourceRegistry;
import com.github.zeng.alt.tenant.liquibase.TenantDataSourceLocator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户业务迁移桥接：把库级数据源注册表接到迁移执行器的数据源定位器上。
 * <p>
 * {@code liquibase-tenant-component} 刻意不依赖 {@code database-tenant-component}，
 * 纯模式级部署不应被迫带上多数据源；本配置只在两者同时在场（即本应用）时桥接。
 * <p>
 * 刻意不用 {@code @ConditionalOnBean(TenantDataSourceRegistry.class)}：
 * 本类是组件扫描的普通配置，解析时各模块 auto-config 的 Bean 定义还没注册，
 * 条件恒为假。用运行时兜底代替——注册表缺席时定位库级数据源才抛错
 * （与模块自带的兜底语义一致），模式级租户不受影响。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(prefix = "alt.tenant.liquibase", name = "enabled", havingValue = "true")
public class TenantMigrationConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDataSourceLocator.class)
    public TenantDataSourceLocator tenantDataSourceLocator(
            ObjectProvider<TenantDataSourceRegistry> registry) {
        return key -> {
            TenantDataSourceRegistry available = registry.getIfAvailable();
            if (available != null) {
                return available.get(key);
            }
            throw new TenantRoutingException(
                    "租户数据源 [" + key + "] 无法定位：库级隔离的迁移需要引入 database-tenant-component");
        };
    }
}
