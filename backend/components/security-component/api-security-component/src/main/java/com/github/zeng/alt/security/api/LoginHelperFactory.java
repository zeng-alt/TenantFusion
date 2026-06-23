package com.github.zeng.alt.security.api;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link LoginHelper} 工厂，按名称选择不同的认证实现.
 * <p>
 * 通过 Spring 自动注入所有 {@link LoginHelper} 实现，支持按名称获取指定的认证方式，
 * 或获取当前可用的唯一认证方式。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public class LoginHelperFactory {

    private final Map<String, LoginHelper> helperMap;

    public LoginHelperFactory(List<LoginHelper> helpers) {
        this.helperMap = helpers.stream()
                .collect(Collectors.toMap(LoginHelper::name, Function.identity()));
    }

    /**
     * 按认证方式名称获取对应的 LoginHelper.
     *
     * @param name 认证方式名称（{@code "jwt"}、{@code "cookie"} 等）
     * @return LoginHelper
     * @throws IllegalArgumentException 未找到指定名称的实现
     */
    public LoginHelper getHelper(String name) {
        LoginHelper helper = helperMap.get(name);
        if (helper == null) {
            throw new IllegalArgumentException("No LoginHelper found for name: " + name
                    + ". Available: " + helperMap.keySet());
        }
        return helper;
    }

    /**
     * 获取当前唯一的 LoginHelper，当仅有一个实现时可直接使用.
     *
     * @return LoginHelper
     * @throws IllegalStateException    没有可用的实现
     * @throws IllegalStateException    有多个实现（此时应使用 {@link #getHelper(String)}）
     */
    public LoginHelper getHelper() {
        if (helperMap.isEmpty()) {
            throw new IllegalStateException("No LoginHelper implementation available. "
                    + "Ensure at least one authentication module (jwt, cookie, etc.) is enabled.");
        }
        if (helperMap.size() > 1) {
            throw new IllegalStateException("Multiple LoginHelper implementations found: "
                    + helperMap.keySet() + ". Use getHelper(name) to select one.");
        }
        return helperMap.values().iterator().next();
    }

    /**
     * 获取所有可用的认证方式名称.
     */
    public java.util.Set<String> getAvailableNames() {
        return helperMap.keySet();
    }
}
