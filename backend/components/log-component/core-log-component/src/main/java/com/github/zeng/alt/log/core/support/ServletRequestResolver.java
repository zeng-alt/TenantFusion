package com.github.zeng.alt.log.core.support;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 基于 {@link HttpServletRequest} 的请求解析器。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class ServletRequestResolver implements RequestResolver {

    @Override
    public String requestURI() {
        try {
            return obtainRequest().getRequestURI();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String method() {
        try {
            return obtainRequest().getMethod();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest obtainRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }
}
