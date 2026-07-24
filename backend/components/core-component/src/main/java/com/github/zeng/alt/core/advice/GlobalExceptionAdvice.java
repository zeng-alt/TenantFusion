package com.github.zeng.alt.core.advice;


import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.exception.BaseI18nException;
import com.github.zeng.alt.api.rest.ErrorResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@CommonsLog
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionAdvice {

    private final ObjectProvider<MessageSourceAccessor> provider;

    @ExceptionHandler(BaseException.class)
    public ErrorResponse exception(BaseException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn(LogMessage.format("%s 请求业务异常: %s", requestURI, e.getMessage()));
        log.debug(LogMessage.format("%s 请求业务异常: %s", requestURI, e.getMessage()), e);
        ErrorResponseEntity errorResponseEntity = ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getCode(), e.getMessage());
        errorResponseEntity.setInstance(URI.create(request.getRequestURI()));
        errorResponseEntity.setTitle(e.getTitle());
        return errorResponseEntity;
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ErrorResponse handle(TransactionSystemException ex, HttpServletRequest request) {
        Throwable root = ex.getMostSpecificCause();
        if (root instanceof BaseException forbidden) {
            return exception(forbidden, request);
        }
        return exception(ex, request);
    }


    @ExceptionHandler(BaseI18nException.class)
    public ErrorResponse exception(BaseI18nException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn(LogMessage.format("%s 请求业务异常: %s", requestURI, e.getMessage()));
        log.debug(LogMessage.format("%s 请求业务异常: %s", requestURI, e.getMessage()), e);
        ErrorResponseEntity errorResponseEntity = ErrorResponseEntity.of(e.getCode(), e.getMessage());
        errorResponseEntity.setInstance(URI.create(request.getRequestURI()));
        errorResponseEntity.setTitle(e.getMessage());
        return errorResponseEntity;
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse exception(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error(LogMessage.format("%s: %s 请求未知异常:", request.getMethod(), requestURI), e);
        ErrorResponseEntity errorResponseEntity = ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        errorResponseEntity.setInstance(URI.create(request.getRequestURI()));
        errorResponseEntity.setTitle(e.getMessage());
        return errorResponseEntity;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse exception(IllegalArgumentException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error(LogMessage.format("%s: %s 请求参数异常: %s", request.getMethod(), requestURI, e.getMessage()));
        log.debug(LogMessage.format("%s: %s 请求参数异常: %s", request.getMethod(), requestURI, e.getMessage()), e);
        ErrorResponseEntity errorResponse = ErrorResponseEntity.of(HttpStatus.BAD_REQUEST, e.getMessage());
        errorResponse.setInstance(URI.create(request.getRequestURI()));
        errorResponse.setTitle(e.getMessage());
        return errorResponse;
    }

    @ExceptionHandler(RuntimeException.class)
    public ErrorResponse exception(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error(LogMessage.format("%s: %s 请求未知运行异常:", request.getMethod(), requestURI), e);
        ErrorResponseEntity errorResponse = ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        errorResponse.setInstance(URI.create(request.getRequestURI()));
        errorResponse.setTitle("请求未知运行异常");
        return errorResponse;
    }
}
