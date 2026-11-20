package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;

import io.vavr.control.Option;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 注解意图注册表，按 {@link MethodClassKey} 缓存（照搬 Spring Security AbstractExpressionAttributeRegistry）。
 * 先解析方法级注解，回退目标类级；无注解缓存 NULL 哨兵并返回 {@link Option#none()}。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacAuthorizeAttributeRegistry {
    private static final AbacAuthorizeAttribute NULL_ATTRIBUTE = new AbacAuthorizeAttribute(null, false, List.of(), null, false);
    private final Map<MethodClassKey, AbacAuthorizeAttribute> cachedAttributes = new ConcurrentHashMap<>();
    private final ParameterNameDiscoverer parameterNames;

    public AbacAuthorizeAttributeRegistry() {
        this(new AbacParameterNameResolver());
    }

    AbacAuthorizeAttributeRegistry(ParameterNameDiscoverer parameterNames) {
        this.parameterNames = parameterNames;
    }

    public Option<AbacAuthorizeAttribute> getAttribute(Method method, Class<?> targetClass) {
        final Class<?> userClass = ClassUtils.getUserClass(targetClass);
        final MethodClassKey key = new MethodClassKey(method, userClass);
        final AbacAuthorizeAttribute attribute =
                cachedAttributes.computeIfAbsent(key, ignored -> resolveAttribute(method, userClass));
        return attribute == NULL_ATTRIBUTE ? Option.none() : Option.of(attribute);
    }

    private AbacAuthorizeAttribute resolveAttribute(Method method, Class<?> targetClass) {
        final Method resolved = BridgeMethodResolver.findBridgedMethod(AopUtils.getMostSpecificMethod(method, targetClass));
        AbacAuthorize annotation = AnnotatedElementUtils.findMergedAnnotation(resolved, AbacAuthorize.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(method, AbacAuthorize.class);
        }
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, AbacAuthorize.class);
        }
        return annotation == null ? NULL_ATTRIBUTE
                : new AbacAuthorizeAttribute(annotation.permission(), annotation.requirePolicy(),
                        List.of(annotation.resources()), metadata(resolved, targetClass), transactional(resolved, targetClass));
    }

    private boolean transactional(Method method, Class<?> targetClass) {
        final Transactional direct = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        final Transactional transaction = direct == null
                ? AnnotatedElementUtils.findMergedAnnotation(targetClass, Transactional.class) : direct;
        return transaction != null;
    }

    private AbacMethodDto metadata(Method method, Class<?> targetClass) {
        final String[] names = parameterNames.getParameterNames(method);
        final Map<String, Integer> indices = new LinkedHashMap<>();
        IntStream.range(0, method.getParameterCount()).forEach(index -> {
            if (names != null && index < names.length) {
                indices.put(names[index], index);
            }
            indices.put("p" + index, index);
            indices.put("a" + index, index);
        });
        return new AbacMethodDto(method, targetClass, indices,
                IntStream.range(0, method.getParameterCount())
                        .mapToObj(index -> ResolvableType.forMethodParameter(method, index, targetClass)).toList(),
                ResolvableType.forMethodReturnType(method, targetClass));
    }
}
