package com.github.zeng.alt.workflow.exception;

import com.github.zeng.alt.api.rest.ErrorResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * 动态表单校验异常处理器。
 * <p>
 * 比全局 {@code GlobalExceptionAdvice} 更优先（{@code @Order(HIGHEST_PRECEDENCE)}），
 * 在 Problem Details 的 {@code errors} 扩展属性中透出字段级错误，供前端定位具体字段。
 *
 * @author zengAlt
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FormValidationAdvice {

    @ExceptionHandler(FormValidationException.class)
    public ErrorResponse handle(FormValidationException e, HttpServletRequest request) {
        ErrorResponseEntity response = ErrorResponseEntity.of(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
        response.setInstance(URI.create(request.getRequestURI()));
        response.setTitle(e.getTitle());
        response.getBody().setProperty("errors", e.getFieldErrors());
        return response;
    }
}
