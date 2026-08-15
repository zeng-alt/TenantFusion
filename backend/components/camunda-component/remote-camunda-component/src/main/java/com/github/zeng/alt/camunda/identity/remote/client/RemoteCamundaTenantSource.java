package com.github.zeng.alt.camunda.identity.remote.client;

import com.github.zeng.alt.camunda.identity.api.CamundaIdentityTenant;
import com.github.zeng.alt.camunda.identity.api.CamundaTenantSource;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityTenant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Remote implementation of {@link CamundaTenantSource}, calling admin service.
 */
@Component
public class RemoteCamundaTenantSource implements CamundaTenantSource {

    private final AdminIdentityClient client;

    public RemoteCamundaTenantSource(AdminIdentityClient client) {
        this.client = client;
    }

    @Override
    public Optional<CamundaIdentityTenant> findTenantById(String tenantId) {
        RemoteCamundaIdentityTenant tenant = client.findTenantById(tenantId);
        if (tenant == null) {
            return Optional.empty();
        }
        return Optional.of(toTenant(tenant));
    }

    @Override
    public List<CamundaIdentityTenant> findTenantsByUsername(String username) {
        List<RemoteCamundaIdentityTenant> tenants = client.findTenantsByUsername(username);
        if (tenants == null) {
            return List.of();
        }
        return tenants.stream().map(this::toTenant).toList();
    }

    @Override
    public List<CamundaIdentityTenant> findAllTenants() {
        List<RemoteCamundaIdentityTenant> tenants = client.findAllTenants();
        if (tenants == null) {
            return List.of();
        }
        return tenants.stream().map(this::toTenant).toList();
    }

    private CamundaIdentityTenant toTenant(RemoteCamundaIdentityTenant tenant) {
        return new CamundaIdentityTenant(tenant.id(), tenant.name());
    }
}
