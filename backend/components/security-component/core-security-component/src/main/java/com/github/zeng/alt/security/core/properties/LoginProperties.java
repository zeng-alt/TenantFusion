package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月30日 09:49
 */
@Data
public class LoginProperties {
	private static final String DEFAULT_LOGOUT_PATH = "/logout";

	private String loginPath = DEFAULT_LOGOUT_PATH;
	private HttpMethod method = HttpMethod.POST;

	public static LoginProperties of(String loginPath, HttpMethod method) {
		LoginProperties loginProperties = new LoginProperties();
		loginProperties.setLoginPath(loginPath);
		loginProperties.setMethod(method);
		return loginProperties;
	}
}
