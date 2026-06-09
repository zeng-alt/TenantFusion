package com.github.zeng.alt.lock.aop;

import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.annotation.Lock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import com.github.zeng.alt.lock.model.LockProperties;
import com.github.zeng.alt.lock.executor.LockExecutor;
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
/**
 * {@link Lock} 娉ㄨВ鐨勬柟娉曟嫤鎴櫒
 *
 * @author zengJiaJun
 * @since 2026骞?6鏈?9鏃?
 * @version 1.0
 */
public class LockInterceptor implements MethodInterceptor, InitializingBean, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(LockInterceptor.class);

    private final Map<Class<? extends LockKeyBuilder>, LockKeyBuilder> keyBuilderMap = new ConcurrentHashMap<>();
    private final Map<Class<? extends LockFailureStrategy>, LockFailureStrategy> failureStrategyMap = new ConcurrentHashMap<>();

    private final LockTemplate lockTemplate;
    private final Collection<LockKeyBuilder> keyBuilders;
    private final Collection<LockFailureStrategy> failureStrategies;
    private final LockProperties lockProperties;
    private final MethodBasedExpressionEvaluator expressionEvaluator;

    private LockOperation defaultLockOperation;
    private BeanFactory beanFactory;

    public LockInterceptor(
            LockTemplate lockTemplate,
            Collection<LockKeyBuilder> keyBuilders,
            Collection<LockFailureStrategy> failureStrategies,
            LockProperties lockProperties,
            MethodBasedExpressionEvaluator expressionEvaluator) {
        this.lockTemplate = lockTemplate;
        this.keyBuilders = keyBuilders;
        this.failureStrategies = failureStrategies;
        this.lockProperties = lockProperties;
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

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

        Lock Lock = AnnotatedElementUtils.findMergedAnnotation(
                invocation.getMethod(), Lock.class);
        if (Lock == null) {
            return invocation.proceed();
        }

        if (StringUtils.hasText(lock.condition())) {
            String conditionResult = expressionEvaluator.getValue(
                    invocation.getMethod(), invocation.getArguments(),
                    lock.condition(), String.class);
            if (!"true".equalsIgnoreCase(conditionResult)) {
                log.debug("Lock condition not met for key [{}], skip locking", lock.name());
                return invocation.proceed();
            }
        }

        LockOperation lockOp = buildLockOperation(Lock);

        String prefix = lockProperties.getLockKeyPrefix() + ":";
        Method method = invocation.getMethod();
        prefix += StringUtils.hasText(lock.name())
                ? lock.name()
                : method.getDeclaringClass().getName() + "." + method.getName();

        String keySuffix = lockOp.lockKeyBuilder.buildKey(invocation, lock.keys());
        String key = prefix + (StringUtils.hasText(keySuffix) ? "#" + keySuffix : "");

        if (log.isDebugEnabled()) {
            log.debug("Generated lock key [{}] for method [{}#{}]",
                    key, method.getDeclaringClass().getSimpleName(), method.getName());
        }

        long expire = lock.expire() > 0 ? lock.expire() : lockProperties.getExpire();

        // LockExecutor.class 为 sentinel 值，表示使用默认执行器
        Class<? extends LockExecutor> executorClass = lock.executor();
        if (executorClass == LockExecutor.class) {
            executorClass = null;
        }

        long acquireTimeout = lock.acquireTimeout() > 0
                ? lock.acquireTimeout() : lockProperties.getAcquireTimeout();

        LockInfo lockInfo = lockTemplate.lock(key, expire, acquireTimeout, executorClass);

        try {
            if (lockInfo != null) {
                log.debug("Lock acquired successfully, key={}, expire={}ms", key, expire);
                return invocation.proceed();
            }

            log.warn("Lock acquisition failed for key [{}]", key);
            lockOp.lockFailureStrategy.onLockFailure(key, method, invocation.getArguments());
            return null;
        } finally {
            if (lockInfo != null && lock.autoRelease()) {
                boolean released = lockTemplate.releaseLock(lockInfo);
                if (released) {
                    log.debug("Lock released successfully, key={}", key);
                } else {
                    log.error("Lock release failed, key={}, value={}", key, lockInfo.getLockValue());
                }
            }
        }
    }

    private LockOperation buildLockOperation(Lock Lock) {
        LockKeyBuilder keyBuilder;
        LockFailureStrategy failureStrategy;

        Class<? extends LockFailureStrategy> failStrategyClass = lock.failStrategy();
        Class<? extends LockKeyBuilder> keyBuilderClass = lock.keyBuilderStrategy();

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
