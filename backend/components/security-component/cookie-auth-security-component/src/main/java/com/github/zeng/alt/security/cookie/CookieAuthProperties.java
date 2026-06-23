package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.LogoutProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

/**
 * Cookie 认证配置属性.
 * <p>
 * 登录路径、参数名等配置复用 {@link com.github.zeng.alt.security.core.properties.UsernameLoginProperties}。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@Data
@ConfigurationProperties(prefix = "security.cookie-auth")
public class CookieAuthProperties {

    private LoginProperties login = LoginProperties.of("/login/cookie", HttpMethod.POST);
    private LogoutProperties logout = LogoutProperties.of("/logout/cookie", HttpMethod.POST);

    /** 开启Cookie登录 */
    private Boolean authentication = false;

    /** 开启Cookie认证，请求要带上Cookie */
    private Boolean validation = false;

    /** Session 缓存过期时间（秒），默认 24 小时 */
    private Long expiration = 86400L;

    /** Cookie 名称 */
    private String cookieName = "SESSION";

    /** Cookie 路径 */
    private String cookiePath = "/";

    /** Cookie 最大存活时间（秒），默认 24 小时 */
    private Integer cookieMaxAge = 86400;

    /** Cookie HttpOnly 标记 */
    private Boolean cookieHttpOnly = true;

    /** Cookie Secure 标记 */
    private Boolean cookieSecure = false;

    /** Cookie 域名（可选） */
    private String cookieDomain;
}
