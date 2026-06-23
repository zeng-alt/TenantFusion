package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月19日 21:37
 */
@Data
@ConfigurationProperties(prefix = "security.logout")
public class LogoutProperties {

    private static final String DEFAULT_LOGOUT_PATH = "/logout";

    private String logoutPath = DEFAULT_LOGOUT_PATH;
    private HttpMethod method = HttpMethod.POST;
}
