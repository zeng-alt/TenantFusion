package com.github.zeng.alt.security.abac.method;

import io.vavr.control.Try;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 优先读取 AOT 生成的参数名，JVM 未生成资源时使用 Spring 参数名发现器。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacParameterNameResolver implements ParameterNameDiscoverer {
    public static final String RESOURCE = "META-INF/abac/method-parameters.properties";
    private final Properties names;
    private final ParameterNameDiscoverer fallback;

    public AbacParameterNameResolver() {
        this(Try.of(() -> PropertiesLoaderUtils.loadAllProperties(RESOURCE, AbacParameterNameResolver.class.getClassLoader()))
                .getOrElseThrow(failure -> new IllegalStateException("Cannot read ABAC AOT parameter names", failure)),
                new DefaultParameterNameDiscoverer());
    }

    AbacParameterNameResolver(Properties names, ParameterNameDiscoverer fallback) {
        this.names = names;
        this.fallback = fallback;
    }

    @Override
    public String[] getParameterNames(Method method) {
        final String value = names.getProperty(key(method));
        if (value == null) {
            return fallback.getParameterNames(method);
        }
        return value.isEmpty() ? new String[0] : value.split(",");
    }

    @Override
    public String[] getParameterNames(Constructor<?> constructor) {
        return fallback.getParameterNames(constructor);
    }

    static String key(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "(" + Arrays.stream(method.getParameterTypes())
                .map(Class::getName).collect(Collectors.joining(",")) + ")";
    }
}
