package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.core.web.AuthorizeHttpRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * @author zengJiaJun
 * @since 2026年06月29日
 * @version 1.0
 */
@Configuration
public class MySecurityConfig {

    @Bean
    public AuthorizeHttpRequestsCustomizer authorizeHttpRequests() {
        return registry ->
                registry
                        .requestMatchers(HttpMethod.POST, "/v1/auth/current-role/switch/{code}/{rememberMe}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/auth/admin").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/v1/user/profile/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/user/detail").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/menu/resource/tree").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/menu/resource/validate").authenticated()
                ;

    }
}
