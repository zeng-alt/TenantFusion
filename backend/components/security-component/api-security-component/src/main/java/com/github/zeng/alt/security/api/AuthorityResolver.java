package com.github.zeng.alt.security.api;

import org.springframework.security.core.Authentication;

import java.util.Set;

/**
 * 为当前认证身份补充权限 code，供通用权限判断与宿主 RBAC 权限源衔接。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AuthorityResolver {

    Set<String> getAuthorities(Authentication authentication);
}
