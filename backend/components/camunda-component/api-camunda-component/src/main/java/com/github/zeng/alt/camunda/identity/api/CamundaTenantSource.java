package com.github.zeng.alt.camunda.identity.api;

import java.util.List;
import java.util.Optional;

/**
 * Camunda 租户 SPI。
 * <p>
 * 由具体存储层实现（如 JPA 读取 admin 的 main_tenant 表），
 * 实现者负责启用/删除状态过滤等业务规则。
 */
public interface CamundaTenantSource {

    /**
     * 按租户 ID 查找租户。
     */
    Optional<CamundaIdentityTenant> findTenantById(String tenantId);

    /**
     * 查找用户所属的全部租户。
     */
    List<CamundaIdentityTenant> findTenantsByUsername(String username);

    /**
     * 查找全部租户。
     */
    List<CamundaIdentityTenant> findAllTenants();
}
