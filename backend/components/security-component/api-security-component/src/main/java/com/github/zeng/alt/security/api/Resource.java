package com.github.zeng.alt.security.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月09日 16:26
 */
public interface Resource {

	public String getUri();

	public HttpMethod getHttpMethod();
	String getMethod();

	default boolean compareTo(HttpServletRequest request) {
		return AntPathRequestMatcher.antMatcher(getHttpMethod(), getUri()).matcher(request).isMatch();
	}

	default String getKey() {
		return null;
	}

}
