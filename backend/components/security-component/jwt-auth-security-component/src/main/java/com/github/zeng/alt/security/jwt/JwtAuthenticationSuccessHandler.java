package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@CommonsLog
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtStorage jwtStorage;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername(securityUser.getUsername());
        event.setStatus("0");
        event.setMessage("登录成功");
        event.setIp(request.getRemoteAddr());
        eventPublisher.publishEvent(event);

        String token = jwtTokenProvider.createToken(securityUser);
        String cacheKey = jwtTokenProvider.getAccessCacheKey(token);

        if (cacheKey != null) {
            jwtStorage.setAccessToken(cacheKey, securityUser.getUsername());
        }

        Map<String, Object> tokenData = new LinkedHashMap<>();
        tokenData.put("accessToken", token);
        tokenData.put("tokenType", "Bearer");
        tokenData.put("expiresIn", jwtProperties.getExpiration());

        if (isRememberMe(request)) {
            String refreshToken = jwtTokenProvider.createRefreshToken(securityUser);

            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey != null) {
                jwtStorage.setRefreshToken(refreshCacheKey, securityUser.getUsername());
            }

            Cookie cookie = new Cookie(jwtProperties.getRefreshCookieName(), refreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath(jwtProperties.getRefreshCookiePath());
            cookie.setMaxAge(jwtProperties.getRememberMeExpiration().intValue());
            response.addCookie(cookie);
        }

        RestResponse<Map<String, Object>> restResponse = RestResponse.success(tokenData);

        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), restResponse);
    }

    private static boolean isRememberMe(HttpServletRequest request) {
        String rememberMe = request.getParameter("rememberMe");
        if (rememberMe == null) {
            Object attr = request.getAttribute("rememberMe");
            if (attr instanceof Boolean) {
                return (Boolean) attr;
            }
            if (attr instanceof String) {
                return "true".equals(attr);
            }
            return false;
        }
        return "true".equals(rememberMe) || "on".equals(rememberMe);
    }
}
