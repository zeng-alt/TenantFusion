package com.github.zeng.alt.admin.query.api.dto;

/**
 * 试算结果，拒绝与表达式错误有独立代码。
 * @param allowed 是否允许
 * @param code 结果代码
 * @param message 结果说明
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacSimulationVO(boolean allowed, String code, String message) { }
