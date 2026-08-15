package com.github.zeng.alt.camunda.identity.remote.dto;

import java.util.List;

/**
 * admin 返回的 Camunda 用户 DTO。
 */
public record RemoteCamundaIdentityUser(
        String id,
        String firstName,
        String lastName,
        String email,
        List<RemoteCamundaIdentityGroup> groups
) {
}
