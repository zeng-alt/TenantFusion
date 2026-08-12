package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.LoginInfoEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * JWT 登录失败处理器.
 * <p>
 * 登录失败时返回 JSON 格式的错误响应。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class JwtAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername(resolveUsername(request));
        event.setStatus("1");
        event.setMessage(exception.getMessage());
        event.setIp(request.getRemoteAddr());
        eventPublisher.publishEvent(event);

        ProblemDetail problemDetail = ProblemDetail.forStatus(600);
        problemDetail.setTitle("用户账号或者密码错误!!");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }

    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(JwtLoginFilter.LOGIN_USERNAME_ATTRIBUTE);
        String username = attr instanceof String ? (String) attr : null;
        if (!StringUtils.hasText(username)) {
            username = request.getParameter("username");
        }
        return username;
    }
}
