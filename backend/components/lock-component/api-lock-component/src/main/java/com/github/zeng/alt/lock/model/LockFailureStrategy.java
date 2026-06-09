package com.github.zeng.alt.lock.model;

import java.lang.reflect.Method;

/**
 * 获取锁失败时的处理策略
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@FunctionalInterface
public interface LockFailureStrategy {

    /**
     * 当加锁失败时的处理逻辑
     *
     * @param key       锁 key
     * @param method    目标方法
     * @param arguments 目标方法参数
     * @throws Throwable 处理过程中可抛出的异常
     */
    void onLockFailure(String key, Method method, Object[] arguments) throws Throwable;
}
