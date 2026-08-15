package com.github.zeng.alt.camunda.identity.api;

/**
 * Camunda 组（角色）身份视图。
 *
 * @param id   组唯一标识（使用 admin 的 role code）
 * @param name 组名称
 * @param type 组类型
 */
public record CamundaIdentityGroup(String id, String name, String type) {
}
