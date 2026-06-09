package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.DistributedLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地内存分布式锁实现（JVM 级别）
 * 基于 java.util.concurrent.locks.ReentrantLock
 * 适用于开发/测试环境或单机部署
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SimpleDistributedLock implements DistributedLock {

    private final String name;
    private final ReentrantLock lock;

    public SimpleDistributedLock(String name) {
        this.name = name;
        this.lock = new ReentrantLock();
    }

    @Override
    public String name() {
        return name;
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
        // Simple 实现不支持自动释放，leaseTime 在此忽略
        return tryLock(waitTime, unit);
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
