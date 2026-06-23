package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月29日 21:57
 */
@Data
@ConfigurationProperties(prefix = "security.username-login")
public class UsernameLoginProperties {

	private Boolean enabled = false;
	private String usernameParameter = "username";
	private String passwordParameter = "password";
}
