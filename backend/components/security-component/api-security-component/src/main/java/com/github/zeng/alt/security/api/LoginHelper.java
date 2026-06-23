package com.github.zeng.alt.security.api;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录认证助手，抽象不同认证方式的登录、登出等操作.
 * <p>
 * 每种认证方式（JWT、Cookie 等）提供一个实现，通过 {@link LoginHelperFactory} 按名称获取。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public interface LoginHelper {

    /**
     * 认证方式名称，如 {@code "jwt"}、{@code "cookie"}.
     */
    String name();

    /**
     * 用户登录.
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含认证用户及各方式特有属性
     */
    LoginResponse login(String username, String password);

    /**
     * 登出当前请求对应的用户.
     *
     * @param request HttpServletRequest，用于提取凭证
     */
    void logout(HttpServletRequest request);

    /**
     * 获取当前已登录用户.
     *
     * @return SecurityUser 或 null（未登录）
     */
    SecurityUser getCurrentUser();
}
