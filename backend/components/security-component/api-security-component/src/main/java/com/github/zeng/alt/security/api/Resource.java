package com.github.zeng.alt.security.api;

import org.springframework.http.HttpMethod;


/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月09日 16:26
 */
public interface Resource {

	public String getUri();

	public HttpMethod getHttpMethod();
	String getMethod();

	default String getKey() {
		return null;
	}

}
