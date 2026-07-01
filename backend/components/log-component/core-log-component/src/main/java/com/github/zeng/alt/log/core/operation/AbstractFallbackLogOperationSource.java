package com.github.zeng.alt.log.core.operation;

import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFallbackLogOperationSource
    implements LogOperationSource {

    /**
     * 空对象缓存，避免重复解析
     */
    private static final LogOperation NULL_OPERATION = new LogOperation();

    /**
     * operation缓存
     */
    private final Map<Object, LogOperation> operationCache =
        new ConcurrentHashMap<>(256);

    @Override
    @Nullable
    public LogOperation getLogOperation(Method method, @Nullable Class<?> targetClass) {

        Object cacheKey = getCacheKey(method, targetClass);

        LogOperation cached = this.operationCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_OPERATION ? null : cached;
        }

        LogOperation operation = computeLogOperation(method, targetClass);

        this.operationCache.put(
            cacheKey,
            operation == null ? NULL_OPERATION : operation
        );

        return operation;
    }

    /**
     * 获取缓存Key
     */
    protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }

    /**
     * 子类真正解析LogOperation
     */
    @Nullable
    protected abstract LogOperation computeLogOperation(
        Method method,
        @Nullable Class<?> targetClass);

}