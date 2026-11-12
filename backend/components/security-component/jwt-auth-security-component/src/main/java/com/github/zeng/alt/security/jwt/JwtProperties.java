package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.LogoutProperties;
import io.vavr.control.Try;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;

/**
 * JWT 认证配置属性.
 * <p>
 * 登录路径、参数名等配置复用 {@link com.github.zeng.alt.security.core.properties.UsernameLoginProperties}。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@Data
@ConfigurationProperties(prefix = "security.jwt-auth")
@Validated
public class JwtProperties {

    @NestedConfigurationProperty
    private LoginProperties login = LoginProperties.of("/login/jwt", HttpMethod.POST);
    @NestedConfigurationProperty
    private LogoutProperties logout = LogoutProperties.of("/logout/jwt", HttpMethod.POST);

    /** 开启 JWT 签发与刷新，并通过本地 UserDetailsService 校验当前账户状态。 */
    private Boolean authentication = false;

    /** 开启 JWT 校验；未开启 authentication 时仅验签及查询共享凭证缓存，不在本地刷新。 */
    private Boolean validation = false;

    /** Base64 编码的 HMAC 签名密钥（至少 256 位），用于 HS256 Access Token */
    @NotBlank(message = "HMAC 签名密钥不能为空")
    private String base64Secret;

    /** Base64 编码的 HMAC 签名密钥（至少 512 位），用于 HS512 Refresh Token，不配置则复用 base64Secret */
    private String base64SecretRefresh;

    /** Token 过期时间（秒），默认 4 小时 */
    private Long expiration = 60 * 60 * 4L;

    /** Token 类型 */
    private String tokenType = "Bearer ";

    /** 记住我功能：Refresh Token 过期时间（秒），默认 7 天 */
    private Long rememberMeExpiration = 60 * 60 * 24 * 7L;

    /** 前端携带 refresh token 的请求头名称（仅用于 API 登录路径） */
    private String refreshTokenHeader = "X-Refresh-Token";

    /** 下发新 access token 的响应头名称 */
    private String newAccessTokenHeader = "X-New-Access-Token";

    /** Cookie 名称 */
    private String refreshCookieName = "refresh_token";

    /** Cookie 路径 */
    private String refreshCookiePath = "/";

    /** Cookie SameSite 属性 */
    private String refreshCookieSameSite = "Lax";

    /** 签发端复用 Access 密钥作 HS512 刷新签名时，解码后须达到 64 字节。 */
    @AssertTrue(message = "JWT 密钥须为合法 Base64：Access 至少 256 位，Refresh 至少 512 位")
    public boolean isSigningKeysValid() {
        final boolean separateRefreshKey = StringUtils.hasText(base64SecretRefresh);
        final int accessBytes = Boolean.TRUE.equals(authentication) && !separateRefreshKey ? 64 : 32;
        return Try.of(() -> Base64.getDecoder().decode(base64Secret).length >= accessBytes
                && (!separateRefreshKey || Base64.getDecoder().decode(base64SecretRefresh).length >= 64))
                .getOrElse(false);
    }

}
