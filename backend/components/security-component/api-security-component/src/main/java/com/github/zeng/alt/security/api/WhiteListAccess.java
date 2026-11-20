package com.github.zeng.alt.security.api;

/**
 * HTTP 请求的公开、登录和普通权限访问级别。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public enum WhiteListAccess {
    PERMIT_ALL,
    AUTHENTICATED,
    PROTECTED
}
