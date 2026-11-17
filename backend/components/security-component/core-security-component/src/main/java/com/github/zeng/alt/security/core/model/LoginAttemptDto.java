package com.github.zeng.alt.security.core.model;

/**
 * 认证前捕获的登录审计上下文，不包含密码或令牌。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record LoginAttemptDto(
        String username,
        String tenantId,
        String ip,
        String userAgent
) {
}
