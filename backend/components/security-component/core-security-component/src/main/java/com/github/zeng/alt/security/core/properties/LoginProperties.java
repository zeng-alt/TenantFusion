package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年09月30日 09:49
 */
@Data
@ConfigurationProperties(prefix = "security.login")
public class LoginProperties {

	private boolean enabled = false;

}
