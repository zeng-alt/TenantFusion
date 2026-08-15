package com.github.zeng.alt.camunda.identity.remote.dto;

/**
 * admin 返回的 Camunda 组 DTO。
 */
public record RemoteCamundaIdentityGroup(
        String id,
        String name,
        String type
) {
}
