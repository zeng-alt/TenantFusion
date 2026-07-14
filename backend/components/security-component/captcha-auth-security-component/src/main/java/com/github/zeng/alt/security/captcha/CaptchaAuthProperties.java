package com.github.zeng.alt.security.captcha;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@Data
@ConfigurationProperties(prefix = "security.captcha-auth")
public class CaptchaAuthProperties {

    private String loginPath = "/login/**";

    private HttpMethod method = HttpMethod.POST;

    private String keyParameter = "captchaKey";

    private String codeParameter = "captchaCode";

    private Boolean validation = false;
}
