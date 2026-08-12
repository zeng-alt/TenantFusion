package com.github.zeng.alt.security.jwt;

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
 * JWT 登录过滤器，支持 JSON body 和表单参数两种登录方式.
 * <p>
 * 认证成功后的处理委托给 {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler}，
 * 由 {@link JwtAuthenticationSuccessHandler} 生成并返回 JWT token。
 * <p>
 * 支持从请求中提取 {@code rememberMe} 参数并存入 request attribute，
 * 供 {@link JwtAuthenticationSuccessHandler} 判断是否发放 refreshToken。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    /**
     * 存放本次尝试登录的用户名，供登录失败处理器记录日志使用。
     */
    public static final String LOGIN_USERNAME_ATTRIBUTE = "loginUsername";

    private final ObjectMapper objectMapper;

    public JwtLoginFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        // 支持 JSON 格式的登录请求
        if (request.getContentType() != null
                && request.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> credentials = objectMapper.readValue(
                        request.getInputStream(), Map.class);
                String username = String.valueOf(credentials.getOrDefault(getUsernameParameter(), ""));
                String password = String.valueOf(credentials.getOrDefault(getPasswordParameter(), ""));
                // 提取 rememberMe 参数
                Object rememberMe = credentials.get("rememberMe");
                if (rememberMe != null) {
                    request.setAttribute("rememberMe", "true".equals(String.valueOf(rememberMe)) || Boolean.TRUE.equals(rememberMe));
                }
                request.setAttribute(LOGIN_USERNAME_ATTRIBUTE, username);
                UsernamePasswordAuthenticationToken authRequest =
                        UsernamePasswordAuthenticationToken.unauthenticated(username, password);
                setDetails(request, authRequest);
                return this.getAuthenticationManager().authenticate(authRequest);
            } catch (IOException e) {
                throw new AuthenticationServiceException("Failed to parse login request body", e);
            }
        }
        // 兼容表单参数方式：提取 rememberMe
        String rememberMe = request.getParameter("rememberMe");
        if (rememberMe != null) {
            request.setAttribute("rememberMe", "true".equals(rememberMe) || "on".equals(rememberMe));
        }
        request.setAttribute(LOGIN_USERNAME_ATTRIBUTE, request.getParameter(getUsernameParameter()));
        return super.attemptAuthentication(request, response);
    }
}
