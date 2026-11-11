package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.Tenant;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;

import java.util.Optional;

/**
 * 老接口 {@link TenantSingleDataSourceProvider} 的本地适配器。
 * <p>
 * 把新模型 {@code TenantMetadata} 翻译回老模型 {@code Tenant}：
 * {@code datasourceKey -> db}、{@code schemaName -> schema}，
 * 档位按新旧枚举同名映射（{@code ROW -> COLUMN}）。
 * 供尚未迁移到 {@code TenantMetadataProvider} 的老调用方
 * （如 {@code UserDetailsServiceImpl}）过渡使用。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class LocalTenantSingleDataSourceProvider implements TenantSingleDataSourceProvider {

    private final LocalTenantMetadataProvider delegate;

    public LocalTenantSingleDataSourceProvider(LocalTenantMetadataProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return delegate.findById(id).map(this::toLegacy);
    }

    private Tenant toLegacy(TenantMetadata metadata) {
        return Tenant.builder()
                .tenantId(metadata.tenantId())
                .db(metadata.dataSourceKey())
                .schema(metadata.schemaName())
                .mode(toLegacyMode(metadata.mode()))
                .build();
    }

    private Tenant.Mode toLegacyMode(TenantMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case DATABASE -> Tenant.Mode.DATABASE;
            case SCHEMA -> Tenant.Mode.SCHEMA;
            case ROW, TABLE, NONE -> Tenant.Mode.COLUMN;
        };
    }
}
