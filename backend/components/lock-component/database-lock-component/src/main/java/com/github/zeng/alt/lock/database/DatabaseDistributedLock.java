package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.DistributedLock;

import java.util.concurrent.TimeUnit;

/**
 * 数据库分布式锁，基于 sys_distributed_lock 表实现
 * 每个实例使用唯一 instanceId，通过 INSERT / UPDATE / DELETE 进行锁管理
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class DatabaseDistributedLock implements DistributedLock {

    private final DatabaseLockTemplate template;
    private final String name;

    public DatabaseDistributedLock(DatabaseLockTemplate template, String name) {
        this.template = template;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean tryLock() {
        return template.acquireLock(name, -1L);
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        do {
            if (template.acquireLock(name, -1L)) {
                return true;
            }
            sleepQuietly(50);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        long expireAtMillis = System.currentTimeMillis() + unit.toMillis(leaseTime);
        do {
            if (template.acquireLock(name, expireAtMillis)) {
                return true;
            }
            sleepQuietly(50);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    @Override
    public void lock() {
        while (!tryLock(5, TimeUnit.SECONDS)) {
            // 持续重试直到获取锁
        }
    }

    @Override
    public void unlock() {
        template.releaseLock(name);
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return template.isHeldByCurrentThread(name);
    }

    @Override
    public boolean isLocked() {
        return template.isLocked(name);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
