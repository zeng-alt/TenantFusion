package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * 匹配方法级或类级 {@link AbacAuthorize} 的静态切点。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacMethodPointcut extends StaticMethodMatcherPointcut {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return AnnotatedElementUtils.hasAnnotation(method, AbacAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, AbacAuthorize.class);
    }
}