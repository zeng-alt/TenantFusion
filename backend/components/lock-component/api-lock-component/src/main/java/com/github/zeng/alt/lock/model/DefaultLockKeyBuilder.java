package com.github.zeng.alt.lock.model;

import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认锁 key 构建器，支持 SpEL 表达式解析
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class DefaultLockKeyBuilder implements LockKeyBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultLockKeyBuilder.class);
    private static final String EMPTY_KEY = "";

    private final MethodBasedExpressionEvaluator expressionEvaluator;

    public DefaultLockKeyBuilder(MethodBasedExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public String buildKey(MethodInvocation invocation, String[] definitionKeys) {
        if (ObjectUtils.isEmpty(definitionKeys)) {
            return EMPTY_KEY;
        }
        String key = getSpelDefinitionKey(definitionKeys, invocation);
        if (log.isDebugEnabled()) {
            log.debug("Built lock key suffix [{}] for method [{}#{}]",
                    key, invocation.getMethod().getDeclaringClass().getSimpleName(),
                    invocation.getMethod().getName());
        }
        return key;
    }

    /**
     * 解析 SpEL 表达式并拼接为 key
     */
    protected String getSpelDefinitionKey(String[] definitionKeys, MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();
        return Stream.of(definitionKeys)
                .filter(StringUtils::hasText)
                .map(k -> expressionEvaluator.getValue(method, arguments, k, String.class))
                .collect(Collectors.joining("."));
    }
}
