package com.github.zeng.alt.admin.query.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 策略编辑输入；租户由服务端当前身份决定。
 * @param permission 权限码
 * @param condition 表达式
 * @param preAuth 是否前置
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyCmd(
    @NotBlank String permission,
    @NotBlank @Size(max = 10000) String condition,
    @NotNull Boolean preAuth
) { }
