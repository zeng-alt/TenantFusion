package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.Tenant;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;

import java.util.Optional;

/**
 * 老接口 {@link TenantSingleDataSourceProvider} 的远程适配器，
 * 与 local 模块的同名适配器语义一致（{@code datasourceKey -> db}）。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class RemoteTenantSingleDataSourceProvider implements TenantSingleDataSourceProvider {

    private final RemoteTenantMetadataProvider delegate;

    public RemoteTenantSingleDataSourceProvider(RemoteTenantMetadataProvider delegate) {
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
