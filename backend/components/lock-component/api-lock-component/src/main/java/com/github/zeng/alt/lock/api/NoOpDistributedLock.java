package com.github.zeng.alt.lock.api;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁空实现，所有操作不执行任何实际锁定逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpDistributedLock implements DistributedLock {

    private final String name;

    public NoOpDistributedLock(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        return true;
    }

    @Override
    public void lock() {
        // no-op
    }

    @Override
    public void unlock() {
        // no-op
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return true;
    }

    @Override
    public boolean isLocked() {
        return false;
    }
}
