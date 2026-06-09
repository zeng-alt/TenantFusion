package com.github.zeng.alt.lock.model;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 锁 key 构建策略
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@FunctionalInterface
public interface LockKeyBuilder {

    /**
     * 构建锁 key 后缀
     *
     * @param invocation     方法调用上下文
     * @param definitionKeys 注解中定义的 key 表达式
     * @return key 后缀（可能为空）
     */
    String buildKey(MethodInvocation invocation, String[] definitionKeys);
}
