package com.github.zeng.alt.security.api;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录响应，包含认证结果及各认证方式特有的属性.
 * <p>
 * JWT 实现会在 {@code attributes} 中放入 {@code accessToken}、{@code tokenType}、{@code expiresIn}；
 * Cookie 实现放入 {@code sessionId}、{@code expiresIn}。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@Data
@Accessors(chain = true)
public class LoginResponse {

    private boolean success;
    private String message;
    private SecurityUser user;
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public static LoginResponse success(SecurityUser user) {
        return new LoginResponse().setSuccess(true).setMessage("登录成功").setUser(user);
    }

    public static LoginResponse failed(String message) {
        return new LoginResponse().setSuccess(false).setMessage(message);
    }

    public LoginResponse attribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }
}
