package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁模板实现
 * 基于 RedissonClient 提供可重入锁、公平锁等分布式锁能力
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonLockTemplate implements LockTemplate {

    private final RedissonClient redissonClient;

    public RedissonLockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String lockName, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockName);
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void execute(String lockName, Runnable runnable) {
        RLock lock = redissonClient.getLock(lockName);
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockName);
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
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
        RLock lock = redissonClient.getLock(lockName);
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
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

    @Override
    public DistributedLock getLock(String lockName) {
        return new RedissonDistributedLock(redissonClient.getLock(lockName));
    }

    @Override
    public DistributedLock getFairLock(String lockName) {
        return new RedissonDistributedLock(redissonClient.getFairLock(lockName));
    }

    @Override
    public boolean tryLock(String lockName) {
        return redissonClient.getLock(lockName).tryLock();
    }

    @Override
    public boolean tryLock(String lockName, long waitTime, TimeUnit unit) {
        try {
            return redissonClient.getLock(lockName).tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void lock(String lockName) {
        redissonClient.getLock(lockName).lock();
    }

    @Override
    public void unlock(String lockName) {
        RLock lock = redissonClient.getLock(lockName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String lockName) {
        return redissonClient.getLock(lockName).isLocked();
    }
}
