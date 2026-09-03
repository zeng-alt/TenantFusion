package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 混合模式独有的启动期校验。
 * <p>
 * 单档位部署不会有这类问题——档位由配置固定，模块是否在场一目了然。混合模式下每个租户
 * 各自声明档位，很容易出现「租户声明了库级隔离，但 {@code database-tenant-component} 没引入」
 * 这种静默失效：请求会落到主数据源，看起来能跑，实际上串了租户的数据。所以必须快速失败。
 * <p>
 * 在 {@link ApplicationReadyEvent} 而不是 Bean 初始化阶段执行，避免与数据源、
 * Liquibase 的初始化顺序纠缠。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class HybridTenantValidator {

    private final TenantRoutingRegistry registry;
    private final ObjectProvider<TenantMetadataProvider> metadataProvider;
    private final ObjectProvider<TenantConnectionStrategy> strategies;
    private final ObjectProvider<TenantConnectionCustomizer> customizers;
    private final ObjectProvider<TenantSqlRewriter> rewriters;

    public HybridTenantValidator(
            TenantRoutingRegistry registry,
            ObjectProvider<TenantMetadataProvider> metadataProvider,
            ObjectProvider<TenantConnectionStrategy> strategies,
            ObjectProvider<TenantConnectionCustomizer> customizers,
            ObjectProvider<TenantSqlRewriter> rewriters) {
        this.registry = registry;
        this.metadataProvider = metadataProvider;
        this.strategies = strategies;
        this.customizers = customizers;
        this.rewriters = rewriters;
    }

    /**
     * 校验每个启用中的租户，其声明的隔离档位都有对应模块支撑。
     *
     * @throws IllegalStateException 存在无法支撑的租户配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        TenantMetadataProvider provider = metadataProvider.getIfAvailable();
        if (provider == null) {
            log.warn("混合模式已启用但没有租户元数据来源，所有租户都会回落到全局档位");
            return;
        }
        List<TenantMetadata> tenants = provider.findAll();
        List<String> problems = new ArrayList<>();
        boolean hasSchemaSupport = customizers.stream().findAny().isPresent();
        boolean hasTableSupport = rewriters.stream().findAny().isPresent();

        for (TenantMetadata tenant : tenants) {
            TenantRouting routing = registry.resolve(tenant.tenantId());
            if (routing.isDatabaseIsolated() && !supportsDatabase(routing)) {
                problems.add("租户 [" + tenant.tenantId() + "] 声明了库级隔离（dataSourceKey="
                        + routing.dataSourceKey() + "），但缺少能提供该数据源的连接策略，"
                        + "请引入 database-tenant-component 并配置 alt.tenant.datasources."
                        + routing.dataSourceKey());
            }
            if (routing.isSchemaIsolated() && !hasSchemaSupport) {
                problems.add("租户 [" + tenant.tenantId() + "] 声明了模式级隔离（schema="
                        + routing.schemaName() + "），但缺少 TenantConnectionCustomizer，"
                        + "请引入 schema-tenant-component 与对应方言模块");
            }
            if (routing.isTableIsolated() && !hasTableSupport) {
                problems.add("租户 [" + tenant.tenantId() + "] 声明了表级隔离（suffix="
                        + routing.tableSuffix() + "），但缺少 TenantSqlRewriter，"
                        + "请引入 table-tenant-component");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "多租户混合模式配置校验失败：\n  - " + String.join("\n  - ", problems));
        }
        log.info("多租户混合模式校验通过，共 " + tenants.size() + " 个启用中的租户");
    }

    /**
     * 是否存在能受理该库级路由的策略。core 的主数据源兜底策略对一切路由都返回 true，
     * 所以这里要排除它——否则库级隔离缺模块时会被兜底策略静默吞掉。
     */
    private boolean supportsDatabase(TenantRouting routing) {
        return strategies.orderedStream()
                .anyMatch(strategy -> !isFallback(strategy) && strategy.supports(routing));
    }

    private boolean isFallback(TenantConnectionStrategy strategy) {
        return strategy.getClass().getName()
                .equals("com.github.zeng.alt.tenant.core.PrimaryDataSourceConnectionStrategy");
    }
}
