package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.model.LockUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 本地内存锁模板实现（JVM 级别）
 * 基于 java.util.concurrent 包，适用于开发/测试环境
 * 注意：不支持跨进程分布式场景
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SimpleLockTemplate implements LockTemplate {

    private final ConcurrentMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LockExecutor<?>> executorMap = new ConcurrentHashMap<>();

    private ReentrantLock getLockInternal(String lockName) {
        return lockMap.computeIfAbsent(lockName, k -> new ReentrantLock());
    }

    // ========== 函数式执行 ==========

    @Override
    public <T> T execute(String lockName, Supplier<T> supplier) {
        ReentrantLock lock = getLockInternal(lockName);
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void execute(String lockName, Runnable runnable) {
        ReentrantLock lock = getLockInternal(lockName);
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        ReentrantLock lock = getLockInternal(lockName);
        try {
            if (lock.tryLock(waitTime, unit)) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        ReentrantLock lock = getLockInternal(lockName);
        try {
            if (lock.tryLock(waitTime, unit)) {
                try {
                    runnable.run();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 锁管理 ==========

    @Override
    public DistributedLock getLock(String lockName) {
        return new SimpleDistributedLock(lockName);
    }

    @Override
    public DistributedLock getFairLock(String lockName) {
        return new SimpleDistributedLock(lockName);
    }

    // ========== 直接操作 ==========

    @Override
    public boolean tryLock(String lockName) {
        return getLockInternal(lockName).tryLock();
    }

    @Override
    public boolean tryLock(String lockName, long waitTime, TimeUnit unit) {
        try {
            return getLockInternal(lockName).tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void lock(String lockName) {
        getLockInternal(lockName).lock();
    }

    @Override
    public void unlock(String lockName) {
        ReentrantLock lock = lockMap.get(lockName);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String lockName) {
        ReentrantLock lock = lockMap.get(lockName);
        return lock != null && lock.isLocked();
    }

    // ========== Lock4j 兼容 API ==========

    @Override
    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        ReentrantLock lock = getLockInternal(key);
        String lockValue = LockUtils.simpleUUID();
        try {
            boolean acquired;
            if (acquireTimeout > 0) {
                acquired = lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS);
            } else {
                lock.lock();
                acquired = true;
            }
            if (acquired) {
                return new LockInfo(key, lockValue, expire, acquireTimeout, 1, lock, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }
        ReentrantLock lock = lockMap.get(lockInfo.getLockKey());
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }
}
