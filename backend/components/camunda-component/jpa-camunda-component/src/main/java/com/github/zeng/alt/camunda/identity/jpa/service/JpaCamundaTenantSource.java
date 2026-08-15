package com.github.zeng.alt.camunda.identity.jpa.service;

import com.github.zeng.alt.camunda.identity.api.CamundaIdentityTenant;
import com.github.zeng.alt.camunda.identity.api.CamundaTenantSource;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainTenantEntity;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainTenantRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 基于 JPA 读取 admin 的 main_tenant 表的 SPI 实现。
 * <p>
 * 用户租户取自其 tenantBy 值（当前 main_user 未持久化该列，默认取 master）。
 */
@Component
@RequiredArgsConstructor
public class JpaCamundaTenantSource implements CamundaTenantSource {

    private final MainTenantRepository tenantRepository;
    private final MainUserRepository userRepository;

    @Override
    public Optional<CamundaIdentityTenant> findTenantById(String tenantId) {
        return tenantRepository.findActiveById(tenantId)
                .map(this::toTenant);
    }

    @Override
    public List<CamundaIdentityTenant> findTenantsByUsername(String username) {
        return userRepository.findActiveByUsername(username)
                .map(u -> tenantRepository.findActiveById(DEFAULT_TENANT_ID)
                        .map(List::of)
                        .orElseGet(List::of))
                .map(tenants -> tenants.stream().map(this::toTenant).toList())
                .orElseGet(List::of);
    }

    @Override
    public List<CamundaIdentityTenant> findAllTenants() {
        return tenantRepository.findAllActive().stream()
                .map(this::toTenant)
                .toList();
    }

    private CamundaIdentityTenant toTenant(MainTenantEntity tenant) {
        return new CamundaIdentityTenant(tenant.getTenantId(), tenant.getTenantName());
    }

    private static final String DEFAULT_TENANT_ID = "master";
}
