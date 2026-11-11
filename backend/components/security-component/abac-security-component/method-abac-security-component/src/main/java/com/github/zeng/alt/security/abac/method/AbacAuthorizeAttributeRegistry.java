package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;

import io.vavr.control.Option;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解意图注册表，按 {@link MethodClassKey} 缓存（照搬 Spring Security AbstractExpressionAttributeRegistry）。
 * 先解析方法级注解，回退目标类级；无注解缓存 NULL 哨兵并返回 {@link Option#none()}。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacAuthorizeAttributeRegistry {
    private static final AbacAuthorizeAttribute NULL_ATTRIBUTE = new AbacAuthorizeAttribute(null, false, List.of());
    private final Map<MethodClassKey, AbacAuthorizeAttribute> cachedAttributes = new ConcurrentHashMap<>();

    public Option<AbacAuthorizeAttribute> getAttribute(Method method, Class<?> targetClass) {
        final MethodClassKey key = new MethodClassKey(method, targetClass);
        final AbacAuthorizeAttribute attribute =
                cachedAttributes.computeIfAbsent(key, ignored -> resolveAttribute(method, targetClass));
        return attribute == NULL_ATTRIBUTE ? Option.none() : Option.of(attribute);
    }

    private AbacAuthorizeAttribute resolveAttribute(Method method, Class<?> targetClass) {
        AbacAuthorize annotation = AnnotatedElementUtils.findMergedAnnotation(method, AbacAuthorize.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, AbacAuthorize.class);
        }
        return annotation == null ? NULL_ATTRIBUTE
                : new AbacAuthorizeAttribute(annotation.permission(), annotation.requirePolicy(),
                        List.of(annotation.resources()));
    }
}