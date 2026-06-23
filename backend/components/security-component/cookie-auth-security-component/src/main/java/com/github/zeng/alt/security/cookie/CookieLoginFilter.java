package com.github.zeng.alt.security.cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Cookie 认证登录过滤器，支持 JSON body 和表单参数两种登录方式.
 * <p>
 * 认证成功后的处理委托给 {@link CookieAuthenticationSuccessHandler}，
 * 由其在缓存中创建会话并设置 Cookie。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public class CookieLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public CookieLoginFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (request.getContentType() != null
                && request.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> credentials = objectMapper.readValue(
                        request.getInputStream(), Map.class);
                String username = credentials.getOrDefault(getUsernameParameter(), "");
                String password = credentials.getOrDefault(getPasswordParameter(), "");
                UsernamePasswordAuthenticationToken authRequest =
                        UsernamePasswordAuthenticationToken.unauthenticated(username, password);
                setDetails(request, authRequest);
                return this.getAuthenticationManager().authenticate(authRequest);
            } catch (IOException e) {
                throw new AuthenticationServiceException("Failed to parse login request body", e);
            }
        }
        return super.attemptAuthentication(request, response);
    }
}
