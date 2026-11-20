package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 远程租户元数据来源：停用过滤与 TTL 缓存验证（内存桩代替 HTTP，
 * 桩返回的即线上传输类型 {@link TenantMetadata} 本身）。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class RemoteTenantMetadataProviderTest {

    private final AtomicInteger calls = new AtomicInteger();

    private final TenantRemoteSource stub = new TenantRemoteSource() {
        @Override
        public TenantMetadata findById(String tenantId) {
            calls.incrementAndGet();
            return switch (tenantId) {
                case "t001" -> new TenantMetadata("t001", "租户一", TenantMode.SCHEMA,
                        null, "sch_t001", null, null, true);
                case "t002" -> new TenantMetadata("t002", "停用租户", TenantMode.ROW,
                        null, null, null, true, false);
                default -> null;
            };
        }

        @Override
        public List<TenantMetadata> findAll() {
            return List.of(
                    new TenantMetadata("t001", "租户一", TenantMode.SCHEMA,
                            null, "sch_t001", null, null, true),
                    new TenantMetadata("t002", "停用租户", TenantMode.ROW,
                            null, null, null, true, false));
        }
    };

    @Test
    @DisplayName("远端元数据直接可用并缓存，缓存期内不再调远端")
    void caches() {
        RemoteTenantMetadataProvider provider =
                new RemoteTenantMetadataProvider(stub, 60_000L);

        Optional<TenantMetadata> first = provider.findById("t001");
        Optional<TenantMetadata> second = provider.findById("t001");

        assertThat(first).isPresent();
        assertThat(first.get().schemaName()).isEqualTo("sch_t001");
        assertThat(second).isPresent();
        assertThat(calls.get()).isEqualTo(1);

        provider.evict("t001");
        provider.findById("t001");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void currentLookupDoesNotTrustCachedEnabledTenant() {
        final var current = new AtomicReference<>(new TenantMetadata("t001", "租户一", TenantMode.ROW,
                null, null, null, true, true));
        final TenantRemoteSource source = new TenantRemoteSource() {
            @Override
            public TenantMetadata findById(String tenantId) {
                return current.get();
            }

            @Override
            public List<TenantMetadata> findAll() {
                return List.of();
            }
        };
        final var provider = new RemoteTenantMetadataProvider(source, 60_000L);
        assertThat(provider.findById("t001")).isPresent();
        current.set(null);
        assertThat(provider.findById("t001")).isPresent();
        assertThat(provider.findCurrentById("t001")).isEmpty();
    }

    @Test
    @DisplayName("停用与不存在的租户返回空且不缓存")
    void disabledIsInvisibleAndNotCached() {
        RemoteTenantMetadataProvider provider =
                new RemoteTenantMetadataProvider(stub, 60_000L);

        assertThat(provider.findById("t002")).isEmpty();
        assertThat(provider.findById("unknown")).isEmpty();
        assertThat(provider.findById(null)).isEmpty();
        assertThat(calls.get()).isEqualTo(2);

        List<TenantMetadata> all = provider.findAll();
        assertThat(all).extracting(TenantMetadata::tenantId).containsExactly("t001");
    }

    @Test
    @DisplayName("老接口适配器语义与 local 一致")
    void legacyAdapterMapsFields() {
        RemoteTenantMetadataProvider provider =
                new RemoteTenantMetadataProvider(stub, 60_000L);
        RemoteTenantSingleDataSourceProvider adapter =
                new RemoteTenantSingleDataSourceProvider(provider);

        assertThat(adapter.findById("t001")).isPresent();
        assertThat(adapter.findById("t001").get().getSchema()).isEqualTo("sch_t001");
        assertThat(adapter.findById("t002")).isEmpty();
    }
}
