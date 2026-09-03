package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.core.TenantProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 混合模式路由：每个租户独立决定四个隔离旋钮，且旋钮可叠加。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class HybridTenantRoutingRegistryTest {

    @Test
    @DisplayName("同一部署里不同租户走不同档位")
    void differentTenantsDifferentModes() {
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                new TenantProperties(),
                TenantMetadataProviderStub.of(
                        // 大客户：独立库
                        new TenantMetadata("big", "大客户", TenantMode.DATABASE,
                                "ds_big", null, null, null, true),
                        // 小客户：共享库 + 行级
                        new TenantMetadata("small", "小客户", TenantMode.ROW,
                                null, null, null, null, true)));

        TenantRouting big = registry.resolve("big");
        TenantRouting small = registry.resolve("small");

        assertThat(big.isDatabaseIsolated()).isTrue();
        assertThat(big.dataSourceKey()).isEqualTo("ds_big");
        assertThat(big.rowIsolated()).isFalse();

        assertThat(small.rowIsolated()).isTrue();
        assertThat(small.isDatabaseIsolated()).isFalse();
    }

    @Test
    @DisplayName("四旋钮可叠加：独立库 + 库内非默认 schema")
    void stacksDatabaseAndSchema() {
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                new TenantProperties(),
                TenantMetadataProviderStub.of(
                        new TenantMetadata("mix", "叠加租户", TenantMode.DATABASE,
                                "ds_mix", "sch_mix", null, null, true)));

        TenantRouting routing = registry.resolve("mix");

        // 这种组合在「四选一」模型里根本表达不出来
        assertThat(routing.isDatabaseIsolated()).isTrue();
        assertThat(routing.isSchemaIsolated()).isTrue();
        assertThat(routing.dataSourceKey()).isEqualTo("ds_mix");
        assertThat(routing.schemaName()).isEqualTo("sch_mix");
    }

    @Test
    @DisplayName("行级旋钮可独立于预设显式开关")
    void rowFlagOverridesPreset() {
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                new TenantProperties(),
                TenantMetadataProviderStub.of(
                        // 独立库，但显式要求同时保留行级判别列，便于日后降级回共享模式
                        new TenantMetadata("both", "双保险", TenantMode.DATABASE,
                                "ds_both", null, null, Boolean.TRUE, true)));

        TenantRouting routing = registry.resolve("both");

        assertThat(routing.isDatabaseIsolated()).isTrue();
        assertThat(routing.rowIsolated()).isTrue();
    }

    @Test
    @DisplayName("元数据未指定档位时回落到全局配置")
    void fallsBackToGlobalMode() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.SCHEMA);
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                properties,
                TenantMetadataProviderStub.of(
                        new TenantMetadata("plain", "普通", null, null, null, null, null, true)));

        TenantRouting routing = registry.resolve("plain");

        assertThat(routing.isSchemaIsolated()).isTrue();
        assertThat(routing.schemaName()).isEqualTo("plain");
    }

    @Test
    @DisplayName("元数据里查不到的租户按全局档位处理")
    void unknownTenantUsesGlobalMode() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.ROW);
        HybridTenantRoutingRegistry registry =
                new HybridTenantRoutingRegistry(properties, TenantMetadataProviderStub.of());

        assertThat(registry.resolve("ghost").rowIsolated()).isTrue();
    }
}
