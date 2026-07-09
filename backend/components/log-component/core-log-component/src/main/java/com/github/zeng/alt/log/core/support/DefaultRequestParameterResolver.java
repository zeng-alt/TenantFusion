package com.github.zeng.alt.log.core.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import lombok.extern.apachecommons.CommonsLog;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 默认请求参数解析器，从 {@link HttpServletRequest} 获取参数并序列化为 JSON。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
public class DefaultRequestParameterResolver implements RequestParameterResolver {

    private final ObjectProvider<ObjectMapper> provider;

    public DefaultRequestParameterResolver(ObjectProvider<ObjectMapper> provider) {
        this.provider = provider;
    }

    public ObjectMapper getMapper() {
        return provider.getObject();
    }

    @Override
    public String resolve(LogInvocation invocation) {
        try {
            // 优先从请求中获取参数
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Map<String, String[]> paramMap = request.getParameterMap();

            if (paramMap.isEmpty()) {
                // 尝试从方法参数获取
                MethodInvocation methodInvocation = invocation.getInvocation();
                Method method = methodInvocation.getMethod();
                Object[] args = methodInvocation.getArguments();
                if (args != null && args.length > 0) {
                    return getMapper().writeValueAsString(args);
                }
                return null;
            }

            // 排除不需要的参数
            String[] excludeParams = invocation.getOperation().getExcludeParams();
            Set<String> excludeSet = excludeParams != null
                    ? new HashSet<>(Arrays.asList(excludeParams))
                    : Collections.emptySet();

            Map<String, String> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (excludeSet.contains(entry.getKey())) continue;
                String[] values = entry.getValue();
                filtered.put(entry.getKey(),
                        values != null ? String.join(",", values) : null);
            }

            return getMapper().writeValueAsString(filtered);
        } catch (Exception e) {
            log.warn("Resolve request parameter failed", e);
            return null;
        }
    }
}
