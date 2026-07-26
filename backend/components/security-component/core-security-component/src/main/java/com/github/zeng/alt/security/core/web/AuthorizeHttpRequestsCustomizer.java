package com.github.zeng.alt.security.core.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
@FunctionalInterface
public interface AuthorizeHttpRequestsCustomizer {

    void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry);

}

