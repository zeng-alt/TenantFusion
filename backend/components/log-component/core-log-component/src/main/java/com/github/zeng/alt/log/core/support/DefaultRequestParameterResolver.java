package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.log.core.operation.LogInvocation;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 默认请求参数解析器，从 {@link HttpServletRequest} 获取参数并序列化为 JSON。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@RequiredArgsConstructor
public class DefaultRequestParameterResolver implements RequestParameterResolver {
    private final RequestResolver requestResolver;
    private final LogValueHelper values;

    @Override
    public String resolve(LogInvocation invocation) {
        final Map<String, Object> parameters = new LinkedHashMap<>(requestResolver.parameters());
        final Parameter[] declarations = AopUtils.getMostSpecificMethod(
                invocation.getInvocation().getMethod(),
                invocation.getInvocation().getThis().getClass()
        ).getParameters();
        final Object[] arguments = invocation.getInvocation().getArguments();
        IntStream.range(0, declarations.length).filter(index -> recordable(arguments[index]))
                .forEach(index -> parameters.put(declarations[index].getName(), arguments[index]));
        return values.toJson(parameters, invocation.getOperation().getExcludeParams());
    }

    private boolean recordable(Object value) {
        if (value instanceof InputStream || value instanceof OutputStream || value instanceof Reader
                || value instanceof Writer || value instanceof MultipartFile || value instanceof MultipartFile[]) {
            return false;
        }
        return value == null || ClassUtils.getAllInterfacesForClassAsSet(value.getClass()).stream()
                .map(Class::getName).noneMatch(name -> name.startsWith("jakarta.servlet.")
                        || name.equals("org.springframework.validation.Errors")
                        || name.equals("org.springframework.web.server.ServerWebExchange")
                        || name.equals("org.reactivestreams.Publisher"));
    }
}
