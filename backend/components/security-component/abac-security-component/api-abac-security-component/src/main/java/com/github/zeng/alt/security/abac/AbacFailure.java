package com.github.zeng.alt.security.abac;

/**
 * 授权或策略管理的预期内失败原因；不携带表达式、用户属性或底层异常信息。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacFailure(String code, String message) {
}
