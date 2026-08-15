package com.github.zeng.alt.camunda.identity.api;

/**
 * Camunda 用户身份视图。
 *
 * @param id        用户唯一标识（使用 admin 的 username）
 * @param firstName 名
 * @param lastName  姓
 * @param email     邮箱
 */
public record CamundaIdentityUser(String id, String firstName, String lastName, String email) {
}
