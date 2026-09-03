package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单档位路由注册表：档位取全局配置，只有具体名字查元数据。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class DefaultTenantRoutingRegistryTest {

    private final AtomicInteger lookups = new AtomicInteger();

    @Test
    @DisplayName("NONE 档位下任何租户都不隔离")
    void noneModeYieldsNoIsolation() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.NONE);
        DefaultTenantRoutingRegistry registry =
                new DefaultTenantRoutingRegistry(properties, provider(null));

        TenantRouting routing = registry.resolve("t001");

        assertThat(routing.isIsolated()).isFalse();
        // NONE 档位不该产生任何元数据查询
        assertThat(lookups.get()).isZero();
    }

    @Test
    @DisplayName("SCHEMA 档位下取元数据里的 schema 名")
    void schemaModeUsesMetadataSchema() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.SCHEMA);
        DefaultTenantRoutingRegistry registry = new DefaultTenantRoutingRegistry(
                properties,
                provider(new TenantMetadata("t001", "租户一", null, null,
                        "sch_t001", null, null, true)));

        TenantRouting routing = registry.resolve("t001");

        assertThat(routing.schemaName()).isEqualTo("sch_t001");
        assertThat(routing.isSchemaIsolated()).isTrue();
        assertThat(routing.isDatabaseIsolated()).isFalse();
        assertThat(routing.rowIsolated()).isFalse();
    }

    @Test
    @DisplayName("元数据缺失时按租户标识推导默认名，保证零配置可用")
    void fallsBackToTenantIdAsName() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.SCHEMA);
        DefaultTenantRoutingRegistry registry =
                new DefaultTenantRoutingRegistry(properties, provider(null));

        assertThat(registry.resolve("t002").schemaName()).isEqualTo("t002");
    }

    @Test
    @DisplayName("单档位注册表不读租户的隔离档位字段")
    void ignoresPerTenantMode() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.ROW);
        // 元数据声明自己是 DATABASE 档位，但单档位注册表应当无视它
        DefaultTenantRoutingRegistry registry = new DefaultTenantRoutingRegistry(
                properties,
                provider(new TenantMetadata("t003", "租户三", TenantMode.DATABASE,
                        "ds_t003", null, null, null, true)));

        TenantRouting routing = registry.resolve("t003");

        assertThat(routing.rowIsolated()).isTrue();
        assertThat(routing.isDatabaseIsolated()).isFalse();
    }

    @Test
    @DisplayName("解析结果被缓存，evict 后重新解析")
    void cachesAndEvicts() {
        TenantProperties properties = new TenantProperties();
        properties.setMode(TenantMode.SCHEMA);
        DefaultTenantRoutingRegistry registry = new DefaultTenantRoutingRegistry(
                properties,
                provider(new TenantMetadata("t004", "租户四", null, null,
                        "sch", null, null, true)));

        registry.resolve("t004");
        registry.resolve("t004");
        assertThat(lookups.get()).isEqualTo(1);

        registry.evict("t004");
        registry.resolve("t004");
        assertThat(lookups.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("空租户标识回落到默认租户")
    void blankTenantFallsBackToDefault() {
        TenantProperties properties = new TenantProperties();
        properties.setDefaultTenantId("master");
        DefaultTenantRoutingRegistry registry =
                new DefaultTenantRoutingRegistry(properties, provider(null));

        assertThat(registry.resolve(null).tenantId()).isEqualTo("master");
        assertThat(registry.resolve("  ").tenantId()).isEqualTo("master");
    }

    /** 返回固定元数据的 provider，并统计查询次数以验证缓存 */
    private ObjectProvider<TenantMetadataProvider> provider(TenantMetadata metadata) {
        TenantMetadataProvider delegate = new TenantMetadataProvider() {
            @Override
            public Optional<TenantMetadata> findById(String tenantId) {
                lookups.incrementAndGet();
                return Optional.ofNullable(metadata)
                        .filter(m -> m.tenantId().equals(tenantId));
            }

            @Override
            public List<TenantMetadata> findAll() {
                return metadata == null ? List.of() : List.of(metadata);
            }
        };
        return new ObjectProvider<>() {
            @Override
            public TenantMetadataProvider getObject(Object... args) {
                return delegate;
            }

            @Override
            public TenantMetadataProvider getObject() {
                return delegate;
            }

            @Override
            public TenantMetadataProvider getIfAvailable() {
                return delegate;
            }

            @Override
            public TenantMetadataProvider getIfUnique() {
                return delegate;
            }
        };
    }
}
