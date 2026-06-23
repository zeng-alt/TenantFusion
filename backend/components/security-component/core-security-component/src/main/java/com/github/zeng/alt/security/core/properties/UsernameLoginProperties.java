package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年09月29日 21:57
 */
@Data
@ConfigurationProperties(prefix = "security.username-login")
public class UsernameLoginProperties {

	private static final String DEFAULT_LOGIN_PATH = "/login/username";

	private Boolean enabled = false;

	private String usernameParameter = "username";

	private String passwordParameter = "password";

	private String loginPath = DEFAULT_LOGIN_PATH;

}
