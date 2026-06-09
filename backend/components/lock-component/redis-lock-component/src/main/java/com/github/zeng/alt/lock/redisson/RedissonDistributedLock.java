package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.DistributedLock;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁适配器，将 RLock 包装为 DistributedLock
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonDistributedLock implements DistributedLock {

    private final RLock lock;

    public RedissonDistributedLock(RLock lock) {
        this.lock = lock;
    }

    @Override
    public String name() {
        return lock.getName();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) {
        try {
            return lock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }
}
