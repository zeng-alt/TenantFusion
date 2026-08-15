package com.github.zeng.alt.camunda.identity.remote.dto;

/**
 * admin 返回的 Camunda 租户 DTO。
 */
public record RemoteCamundaIdentityTenant(
        String id,
        String name
) {
}
