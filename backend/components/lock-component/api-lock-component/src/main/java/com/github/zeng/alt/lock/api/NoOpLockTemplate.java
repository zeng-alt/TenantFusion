package com.github.zeng.alt.lock.api;

import java.util.concurrent.TimeUnit;
import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockInfo;

import java.util.function.Supplier;

/**
 * 方法为空实现，则不会执行接口默认方法
 * 如果未配置接口调用时会使用默认 fallback
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpLockTemplate implements LockTemplate {

    @Override
    public <T> T execute(String lockName, Supplier<T> supplier) {
        return supplier.get();
    }

    @Override
    public void execute(String lockName, Runnable runnable) {
        runnable.run();
    }

    @Override
    public <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        return supplier.get();
    }

    @Override
    public void execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        runnable.run();
    }

    @Override
    public DistributedLock getLock(String lockName) {
        return new NoOpDistributedLock(lockName);
    }

    @Override
    public DistributedLock getFairLock(String lockName) {
        return new NoOpDistributedLock(lockName);
    }

    @Override
    public boolean tryLock(String lockName) {
        return true;
    }

    @Override
    public boolean tryLock(String lockName, long waitTime, TimeUnit unit) {
        return true;
    }

    @Override
    public void lock(String lockName) {
        // no-op
    }

    @Override
    public void unlock(String lockName) {
        // no-op
    }

    @Override
    public boolean isLocked(String lockName) {
        return false;
    }

    @Override
    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        return new LockInfo(key, "noop", expire, acquireTimeout, 1, null, null);
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        return true;
    }
}
