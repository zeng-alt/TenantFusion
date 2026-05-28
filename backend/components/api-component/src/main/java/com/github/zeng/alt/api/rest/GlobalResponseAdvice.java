package com.github.zeng.alt.api.rest;


import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @author zengJiaJun
 * @crateTime 2026年04月02日 17:04
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final MessageSourceAccessor messageSourceAccessor;

    public GlobalResponseAdvice(MessageSourceAccessor messageSourceAccessor) {
        this.messageSourceAccessor = messageSourceAccessor;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();

        return !org.springframework.http.HttpEntity.class.isAssignableFrom(type)
                && !HttpEntity.class.isAssignableFrom(type)
                && !HttpEntityStatus.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        if (body instanceof String s) {
            if ("ok".equals(s)) {
                return RestResponse.success();
            } else if ("fail".equals(s)) {
                return ErrorResponseEntity.of();
            } else if (s.startsWith("ok:")) {
                String message = extractMessage(s);
                return RestResponse.success().message(message);
            } else if (s.startsWith("fail:")) {
                String msg = extractMessage(s);
                String message = messageSourceAccessor.getMessage("GlobalExceptionAdvice.exception.error", msg);
                return ErrorResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, message, msg);
            } else {
                return body;
            }
        }

        return body;
    }

    private String extractMessage(String body) {
        String message = body.substring(4);
        if (isEnclosedInBraces(message)) {
            message = messageSourceAccessor.getMessage(getStringInsideBraces(message), message);
        }
        return message;
    }

    public String getStringInsideBraces(String str) {
        if (str == null || !str.startsWith("{") || !str.endsWith("}")) {
            return str;
        }
        // 获取 { 和 } 的位置
        int start = str.indexOf("{") + 1;
        int end = str.indexOf("}", start);
        return str.substring(start, end);
    }

    /**
     * 判断字符串是否被{}包含
     */
    public boolean isEnclosedInBraces(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        return str.startsWith("{") && str.endsWith("}");
    }
}
