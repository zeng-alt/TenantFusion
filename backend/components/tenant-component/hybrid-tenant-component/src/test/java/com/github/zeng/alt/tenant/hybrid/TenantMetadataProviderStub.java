package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 测试用的租户元数据桩。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
final class TenantMetadataProviderStub implements TenantMetadataProvider {

    private final List<TenantMetadata> tenants;

    private TenantMetadataProviderStub(List<TenantMetadata> tenants) {
        this.tenants = tenants;
    }

    static ObjectProvider<TenantMetadataProvider> of(TenantMetadata... tenants) {
        TenantMetadataProvider delegate = new TenantMetadataProviderStub(Arrays.asList(tenants));
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

    @Override
    public Optional<TenantMetadata> findById(String tenantId) {
        return tenants.stream().filter(t -> t.tenantId().equals(tenantId)).findFirst();
    }

    @Override
    public List<TenantMetadata> findAll() {
        return tenants;
    }
}
