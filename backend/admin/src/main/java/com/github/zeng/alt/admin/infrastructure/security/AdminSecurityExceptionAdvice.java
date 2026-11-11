package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.api.rest.ErrorResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * 方法级 ABAC 拒绝（{@link AccessDeniedException}）渲染为 403 ProblemDetail。
 * 优先级高于 GlobalExceptionAdvice 的 RuntimeException 兜底，避免授权拒绝被误渲染为 500。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminSecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        log.warn(LogMessage.format("%s 请求授权拒绝: %s", requestUri, e.getMessage()));
        final ErrorResponseEntity response = ErrorResponseEntity.of(HttpStatus.FORBIDDEN, e.getMessage());
        response.setInstance(URI.create(requestUri));
        response.setTitle("访问被拒绝");
        return response;
    }
}