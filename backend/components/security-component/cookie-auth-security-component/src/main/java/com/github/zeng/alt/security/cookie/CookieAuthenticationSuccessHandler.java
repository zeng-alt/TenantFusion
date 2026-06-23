package com.github.zeng.alt.security.cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Cookie 认证成功处理器.
 * <p>
 * 登录成功后创建会话存入缓存，设置 Session Cookie，返回 JSON 成功响应。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class CookieAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final CookieAuthProperties cookieAuthProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        String sessionId = sessionManager.createSession(securityUser);

        Cookie sessionCookie = new Cookie(cookieAuthProperties.getCookieName(), sessionId);
        sessionCookie.setPath(cookieAuthProperties.getCookiePath());
        sessionCookie.setHttpOnly(cookieAuthProperties.getCookieHttpOnly());
        sessionCookie.setSecure(cookieAuthProperties.getCookieSecure());
        sessionCookie.setMaxAge(cookieAuthProperties.getCookieMaxAge());
        if (cookieAuthProperties.getCookieDomain() != null && !cookieAuthProperties.getCookieDomain().isBlank()) {
            sessionCookie.setDomain(cookieAuthProperties.getCookieDomain());
        }
        response.addCookie(sessionCookie);

        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), RestResponse.success());
    }
}
