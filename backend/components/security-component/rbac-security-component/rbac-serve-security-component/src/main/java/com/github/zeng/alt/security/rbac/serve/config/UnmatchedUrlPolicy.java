package com.github.zeng.alt.security.rbac.serve.config;

/**
 * URL 无匹配权限时的处理模式。「无匹配」指 URL 查不到所需权限码（未注册路由模板，或模板未配权限）。
 * DENY 拒绝（默认，安全默认）；PERMIT 放行。已配权限码的 URL 不受此模式影响，始终要求用户持有该码。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public enum UnmatchedUrlPolicy {
    DENY,
    PERMIT
}