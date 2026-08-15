package com.github.zeng.alt.camunda.identity.api;

/**
 * Camunda 租户身份视图。
 *
 * @param id   租户唯一标识（使用 admin 的 tenantId）
 * @param name 租户名称
 */
public record CamundaIdentityTenant(String id, String name) {
}
