package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.LogoutProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;

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

    private LoginProperties login = LoginProperties.of("/login/jwt", HttpMethod.POST);
    private LogoutProperties logout = LogoutProperties.of("/logout/jwt", HttpMethod.POST);

    /** 开启jwt登录 */
    private Boolean authentication = false;

    /** 开启jwt认证，请求要带上jwt */
    private Boolean validation = false;

    /** Base64 编码的 HMAC 签名密钥（至少 256 位） */
    @NotBlank(message = "HMAC 签名密钥不能为空")
    @Length(min = 256, message = "HMAC 签名密钥长度最小为【256】位")
    private String base64Secret;

    /** Token 过期时间（秒），默认 24 小时 */
    private Long expiration = 60 * 60 * 24L;

    /** Token 类型 */
    private String tokenType = "Bearer";

}
