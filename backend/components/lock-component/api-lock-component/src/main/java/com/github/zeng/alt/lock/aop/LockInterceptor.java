package com.github.zeng.alt.lock.aop;

import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.annotation.Lock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import com.github.zeng.alt.lock.config.LockProperties;
import com.github.zeng.alt.lock.executor.LockExecutor;
import lombok.extern.apachecommons.CommonsLog;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
/**
 * {@link Lock} 注解的方法拦截器
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@CommonsLog
public class LockInterceptor implements MethodInterceptor, BeanFactoryAware {

    private final Map<Class<? extends LockKeyBuilder>, LockKeyBuilder> keyBuilderMap =
            new ConcurrentHashMap<>();

    private final Map<Class<? extends LockFailureStrategy>, LockFailureStrategy> failureStrategyMap =
            new ConcurrentHashMap<>();

    private final ObjectProvider<LockTemplate> lockTemplateProvider;

    private final ObjectProvider<List<LockKeyBuilder>> keyBuildersProvider;

    private final ObjectProvider<List<LockFailureStrategy>> failureStrategiesProvider;

    private final ObjectProvider<LockProperties> lockPropertiesProvider;

    private final ObjectProvider<MethodBasedExpressionEvaluator> expressionEvaluatorProvider;


    private LockOperation defaultLockOperation;

    private BeanFactory beanFactory;


    public LockInterceptor(
            ObjectProvider<LockTemplate> lockTemplateProvider,
            ObjectProvider<List<LockKeyBuilder>> keyBuildersProvider,
            ObjectProvider<List<LockFailureStrategy>> failureStrategiesProvider,
            ObjectProvider<LockProperties> lockPropertiesProvider,
            ObjectProvider<MethodBasedExpressionEvaluator> expressionEvaluatorProvider) {

        this.lockTemplateProvider = lockTemplateProvider;
        this.keyBuildersProvider = keyBuildersProvider;
        this.failureStrategiesProvider = failureStrategiesProvider;
        this.lockPropertiesProvider = lockPropertiesProvider;
        this.expressionEvaluatorProvider = expressionEvaluatorProvider;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        LockTemplate lockTemplate = lockTemplateProvider.getObject();
        LockProperties lockProperties = lockPropertiesProvider.getObject();
        MethodBasedExpressionEvaluator expressionEvaluator =
                expressionEvaluatorProvider.getObject();


        Class<?> cls = AopProxyUtils.ultimateTargetClass(
                Objects.requireNonNull(invocation.getThis()));


        if (!cls.equals(invocation.getThis().getClass())) {
            return invocation.proceed();
        }


        Lock lock = AnnotatedElementUtils.findMergedAnnotation(
                invocation.getMethod(),
                Lock.class);


        if (lock == null) {
            return invocation.proceed();
        }


        if (StringUtils.hasText(lock.condition())) {

            String conditionResult = expressionEvaluator.getValue(
                    invocation.getMethod(),
                    invocation.getArguments(),
                    lock.condition(),
                    String.class);


            if (!"true".equalsIgnoreCase(conditionResult)) {
                return invocation.proceed();
            }
        }


        LockOperation lockOp = buildLockOperation(lock);


        String prefix =
                lockProperties.getLockKeyPrefix() + ":";


        Method method = invocation.getMethod();


        prefix += StringUtils.hasText(lock.name())
                ? lock.name()
                : method.getDeclaringClass().getName()
                  + "."
                  + method.getName();


        String keySuffix =
                lockOp.lockKeyBuilder.buildKey(
                        invocation,
                        lock.keys());


        String key =
                prefix +
                        (StringUtils.hasText(keySuffix)
                                ? "#" + keySuffix
                                : "");


        long expire =
                lock.expire() > 0
                        ? lock.expire()
                        : lockProperties.getExpire();


        Class<? extends LockExecutor> executorClass =
                lock.executor();


        if (executorClass == LockExecutor.class) {
            executorClass = null;
        }


        long acquireTimeout =
                lock.acquireTimeout() > 0
                        ? lock.acquireTimeout()
                        : lockProperties.getAcquireTimeout();


        LockInfo lockInfo =
                lockTemplate.lock(
                        key,
                        expire,
                        acquireTimeout,
                        executorClass);


        try {

            if (lockInfo != null) {
                return invocation.proceed();
            }


            lockOp.lockFailureStrategy
                    .onLockFailure(
                            key,
                            method,
                            invocation.getArguments());


            return null;


        } finally {

            if (lockInfo != null && lock.autoRelease()) {

                lockTemplate.releaseLock(lockInfo);

            }
        }
    }

    private LockOperation buildLockOperation(Lock lock) {
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
