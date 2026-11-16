package com.github.zeng.alt.excel.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 在进程内传递已有认证；每次恢复创建独立 SecurityContext，并还原执行线程原对象。
 * UserContextHolder 从该上下文读取用户，无需另建用户副本或伪造认证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class SecurityExcelContextHandler implements ExcelContextHandler {

    @Override
    public Snapshot capture() {
        return capture(SecurityContextHolder.getContext());
    }

    /** 从 WebFlux Reactor Context 中已有的安全上下文捕获认证。 */
    public Snapshot capture(SecurityContext context) {
        final Authentication authentication = context.getAuthentication();
        return () -> {
            final SecurityContext previous = SecurityContextHolder.getContext();
            final SecurityContext restored = SecurityContextHolder.createEmptyContext();
            restored.setAuthentication(authentication);
            SecurityContextHolder.setContext(restored);
            return () -> SecurityContextHolder.setContext(previous);
        };
    }
}
