package com.github.zeng.alt.core.advice;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(
            WebRequest request,
            ErrorAttributeOptions options) {

        Map<String, Object> map = super.getErrorAttributes(request, options);

        return Map.of(
                "code", map.get("status"),
                "status", map.get("status"),
                "message", map.get("error")
        );
    }
}