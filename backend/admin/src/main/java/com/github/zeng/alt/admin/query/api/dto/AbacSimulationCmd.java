package com.github.zeng.alt.admin.query.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 仅用于管理员试算的模拟数据，不作为业务请求身份。
 * @param permission 权限码
 * @param condition 候选表达式
 * @param preAuth 是否前置
 * @param method 方法元数据标识
 * @param context JSON 形状的上下文样本
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacSimulationCmd(
    @NotBlank String permission,
    @NotBlank @Size(max = 10000) String condition,
    @NotNull Boolean preAuth,
    @NotBlank String method,
    @NotNull Map<String, Object> context
) { }
