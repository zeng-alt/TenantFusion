package com.github.zeng.alt.security.api;

import org.springframework.security.authorization.ReactiveAuthorizationManager;

/**
 * 基础 OR 授权通过后必须满足的响应式 AND 约束，不参与 Provider 的 OR 授权。
 * 约束仅在明确允许时通过，返回 {@code null} 或空发布者表示拒绝。
 *
 * @param <T> 授权上下文类型
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ReactiveAuthorizationConstraint<T> extends ReactiveAuthorizationManager<T> {
}
