package com.github.zeng.alt.admin.query.api.dto;

/**
 * 可配置策略的权限。
 * @param id 权限标识
 * @param code 权限码
 * @param name 权限名称
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPermissionVO(String id, String code, String name) { }
