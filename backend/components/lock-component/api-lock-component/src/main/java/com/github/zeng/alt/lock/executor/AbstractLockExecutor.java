package com.github.zeng.alt.lock.executor;

/**
 * 抽象锁执行器基类
 *
 * @param <T> 锁实例类型
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public abstract class AbstractLockExecutor<T> implements LockExecutor<T> {

    /**
     * 根据加锁结果获取锁实例
     *
     * @param locked       是否加锁成功
     * @param lockInstance 锁实例
     * @return 加锁成功返回锁实例，否则返回 null
     */
    protected T obtainLockInstance(boolean locked, T lockInstance) {
        return locked ? lockInstance : null;
    }
}
