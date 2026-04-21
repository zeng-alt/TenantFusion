package com.github.zeng.alt.core.advice;


import com.github.zeng.alt.api.rest.ErrorResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月31日 21:20
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalServletExceptionAdvice {

    @ExceptionHandler(NoResourceFoundException.class)
    public ErrorResponse exception(NoResourceFoundException e, HttpServletRequest request, HttpServletResponse response) {
        ErrorResponseEntity errorResponse = ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND.value(), e.getMessage());
        errorResponse.setInstance(URI.create(request.getRequestURI()));
        errorResponse.setTitle("资源不存在");
        return errorResponse;
    }
}
