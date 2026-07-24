package com.github.zeng.alt.domain.advice;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.ErrorResponseEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.StringJoiner;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
@CommonsLog
@RestControllerAdvice
@RequiredArgsConstructor
public class DomainExceptionAdvice {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponseEntity handle(DataIntegrityViolationException e) {
        ErrorResponseEntity errorResponseEntity = ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, 600, e.getMessage());
        errorResponseEntity.setTitle("数据已存在, 检查是否存在相同的数据!!");
        return errorResponseEntity;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponseEntity handle(ConstraintViolationException e) {
        StringJoiner joiner = new StringJoiner("\n");
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            joiner.add(violation.getMessage());
        }
        ErrorResponseEntity errorResponseEntity = ErrorResponseEntity.of(HttpStatus.BAD_REQUEST, 400, e.getMessage());
        errorResponseEntity.setTitle("校验失败");
        errorResponseEntity.setDetail(joiner.toString());
        return errorResponseEntity;
    }

}
