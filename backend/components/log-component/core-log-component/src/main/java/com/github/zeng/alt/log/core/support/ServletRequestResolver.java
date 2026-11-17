package com.github.zeng.alt.log.core.support;

import io.vavr.control.Option;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        return request().map(HttpServletRequest::getRequestURI).getOrElse("");
    }

    @Override
    public String method() {
        return request().map(HttpServletRequest::getMethod).getOrElse("");
    }

    @Override
    public Map<String, String[]> parameters() {
        return request().map(HttpServletRequest::getParameterMap).getOrElse(Map::of);
    }

    private Option<HttpServletRequest> request() {
        return Option.of(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast).map(ServletRequestAttributes::getRequest);
    }
}
