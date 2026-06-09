package com.github.zeng.alt.lock.aop;

import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.annotation.AltLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import com.github.zeng.alt.lock.model.LockProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

    @Override
    public void afterPropertiesSet() {
        for (LockKeyBuilder builder : keyBuilders) {
            keyBuilderMap.put(builder.getClass(), builder);
        }
        for (LockFailureStrategy strategy : failureStrategies) {
            failureStrategyMap.put(strategy.getClass(), strategy);
        }

        LockKeyBuilder defaultKeyBuilder = resolvePrimaryComponent(
                lockProperties.getPrimaryKeyBuilder(), keyBuilderMap, keyBuilders, LockKeyBuilder.class);
        LockFailureStrategy defaultFailureStrategy = resolvePrimaryComponent(
                lockProperties.getPrimaryFailStrategy(), failureStrategyMap, failureStrategies, LockFailureStrategy.class);

        this.defaultLockOperation = new LockOperation(defaultKeyBuilder, defaultFailureStrategy);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> cls = AopProxyUtils.ultimateTargetClass(Objects.requireNonNull(invocation.getThis()));
        if (!cls.equals(invocation.getThis().getClass())) {
            return invocation.proceed();
        }

        AltLock altLock = AnnotatedElementUtils.findMergedAnnotation(
                invocation.getMethod(), AltLock.class);
        if (altLock == null) {
            return invocation.proceed();
        }

        if (StringUtils.hasText(altLock.condition())) {
            String conditionResult = expressionEvaluator.getValue(
                    invocation.getMethod(), invocation.getArguments(),
                    altLock.condition(), String.class);
            if (!"true".equalsIgnoreCase(conditionResult)) {
                log.debug("Lock condition not met for key [{}], skip locking", altLock.name());
                return invocation.proceed();
            }
        }

        LockOperation lockOp = buildLockOperation(altLock);

        String prefix = lockProperties.getLockKeyPrefix() + ":";
        Method method = invocation.getMethod();
        prefix += StringUtils.hasText(altLock.name())
                ? altLock.name()
                : method.getDeclaringClass().getName() + "." + method.getName();

        String keySuffix = lockOp.lockKeyBuilder.buildKey(invocation, altLock.keys());
        String key = prefix + (StringUtils.hasText(keySuffix) ? "#" + keySuffix : "");

        if (log.isDebugEnabled()) {
            log.debug("Generated lock key [{}] for method [{}#{}]",
                    key, method.getDeclaringClass().getSimpleName(), method.getName());
        }

        long expire = altLock.expire() > 0 ? altLock.expire() : lockProperties.getExpire();
        long acquireTimeout = altLock.acquireTimeout() > 0
                ? altLock.acquireTimeout() : lockProperties.getAcquireTimeout();

        LockInfo lockInfo = lockTemplate.lock(key, expire, acquireTimeout, altLock.executor());

        try {
            if (lockInfo != null) {
                log.debug("Lock acquired successfully, key={}, expire={}ms", key, expire);
                return invocation.proceed();
            }

            log.warn("Lock acquisition failed for key [{}]", key);
            lockOp.lockFailureStrategy.onLockFailure(key, method, invocation.getArguments());
            return null;
        } finally {
            if (lockInfo != null && altLock.autoRelease()) {
                boolean released = lockTemplate.releaseLock(lockInfo);
                if (released) {
                    log.debug("Lock released successfully, key={}", key);
                } else {
                    log.error("Lock release failed, key={}, value={}", key, lockInfo.getLockValue());
                }

    private LockOperation buildLockOperation(AltLock altLock) {
        LockKeyBuilder keyBuilder;
        LockFailureStrategy failureStrategy;

        Class<? extends LockFailureStrategy> failStrategyClass = altLock.failStrategy();
        Class<? extends LockKeyBuilder> keyBuilderClass = altLock.keyBuilderStrategy();

        if (keyBuilderClass == null || keyBuilderClass == LockKeyBuilder.class) {
            keyBuilder = defaultLockOperation.lockKeyBuilder;
        } else {
            keyBuilder = keyBuilderMap.get(keyBuilderClass);
            if (keyBuilder == null) {
                keyBuilder = beanFactory.getBean(keyBuilderClass);
                keyBuilderMap.put(keyBuilderClass, keyBuilder);
            }
        }

        if (failStrategyClass == null || failStrategyClass == LockFailureStrategy.class) {
            failureStrategy = defaultLockOperation.lockFailureStrategy;
        } else {
            failureStrategy = failureStrategyMap.get(failStrategyClass);
            if (failureStrategy == null) {
                failureStrategy = beanFactory.getBean(failStrategyClass);
                failureStrategyMap.put(failStrategyClass, failureStrategy);
            }
        }

        return new LockOperation(keyBuilder, failureStrategy);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolvePrimaryComponent(
            Class<? extends T> primaryType,
            Map<Class<? extends T>, T> instanceMap,
            Collection<T> instances,
            Class<T> type) {
        if (primaryType != null) {
            T instance = instanceMap.get(primaryType);
            if (instance != null) {
                return instance;
            }
            return beanFactory.getBean(primaryType);
        }
        return instances.stream()
                .min(AnnotationAwareOrderComparator.INSTANCE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No component of type " + type.getName() + " found"));
    }

    private static class LockOperation {
        private final LockKeyBuilder lockKeyBuilder;
        private final LockFailureStrategy lockFailureStrategy;

        LockOperation(LockKeyBuilder lockKeyBuilder, LockFailureStrategy lockFailureStrategy) {
            this.lockKeyBuilder = lockKeyBuilder;
            this.lockFailureStrategy = lockFailureStrategy;
        }
    }
}