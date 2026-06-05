package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 数据库分布式锁模板实现
 * 基于 JdbcClient 和 sys_distributed_lock 表实现跨进程分布式锁
 *
 * <h3>锁获取策略</h3>
 * <ol>
 *   <li>尝试 INSERT 新记录（持有锁）</li>
 *   <li>如果主键冲突（锁已存在），检查是否已过期</li>
 *   <li>如果已过期，尝试 UPDATE 抢占锁</li>
 * </ol>
 *
 * <h3>表结构</h3>
 * <pre>{@code
 * CREATE TABLE sys_distributed_lock (
 *     lock_name   VARCHAR(255) PRIMARY KEY,
 *     instance_id VARCHAR(255) NOT NULL,
 *     locked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     expire_at   TIMESTAMP NULL
 * );
 * }</pre>
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class DatabaseLockTemplate implements LockTemplate {

    private final JdbcClient jdbcClient;
    private final String instanceId;
    private final Map<String, Thread> lockHolders;

    public DatabaseLockTemplate(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        this.instanceId = UUID.randomUUID().toString().replace("-", "");
        this.lockHolders = new ConcurrentHashMap<>();
    }


    /**
     * 尝试获取锁
     *
     * @param lockName       锁名称
     * @param expireAtMillis 过期时间戳（毫秒），-1 表示不过期
     * @return true 获取成功
     */
    boolean acquireLock(String lockName, long expireAtMillis) {
        LocalDateTime expireAt = expireAtMillis > 0
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expireAtMillis), ZoneId.systemDefault())
                : null;

        // 1. 尝试插入新记录
        int inserted = jdbcClient.sql("""
                INSERT INTO sys_distributed_lock (lock_name, instance_id, locked_at, expire_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?)
                """)
                .params(lockName, instanceId, expireAt)
                .update();

        if (inserted > 0) {
            lockHolders.put(lockName, Thread.currentThread());
            return true;
        }

        // 2. 插入失败，检查是否已过期，尝试抢占
        int updated = jdbcClient.sql("""
                UPDATE sys_distributed_lock
                SET instance_id = ?, locked_at = CURRENT_TIMESTAMP, expire_at = ?
                WHERE lock_name = ? AND expire_at IS NOT NULL AND expire_at <= CURRENT_TIMESTAMP
                """)
                .params(instanceId, expireAt, lockName)
                .update();

        if (updated > 0) {
            lockHolders.put(lockName, Thread.currentThread());
            return true;
        }

        return false;
    }

    /**
     * 释放锁（仅当持有者匹配时）
     */
    void releaseLock(String lockName) {
        jdbcClient.sql("DELETE FROM sys_distributed_lock WHERE lock_name = ? AND instance_id = ?")
                .params(lockName, instanceId)
                .update();
        lockHolders.remove(lockName);
    }

    /**
     * 当前线程是否持有该锁
     */
    boolean isHeldByCurrentThread(String lockName) {
        return Thread.currentThread().equals(lockHolders.get(lockName));
    }

    /**
     * 锁是否被任何实例持有
     */
    public boolean isLocked(String lockName) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*) FROM sys_distributed_lock
                WHERE lock_name = ? AND (expire_at IS NULL OR expire_at > CURRENT_TIMESTAMP)
                """)
                .param(lockName)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    // ========== LockTemplate 实现 ==========

    @Override
    public <T> T execute(String lockName, Supplier<T> supplier) {
        DistributedLock lock = getLock(lockName);
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void execute(String lockName, Runnable runnable) {
        DistributedLock lock = getLock(lockName);
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        DistributedLock lock = getLock(lockName);
        if (lock.tryLock(waitTime, leaseTime, unit)) {
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public void execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        DistributedLock lock = getLock(lockName);
        if (lock.tryLock(waitTime, leaseTime, unit)) {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public DistributedLock getLock(String lockName) {
        return new DatabaseDistributedLock(this, lockName);
    }

    @Override
    public DistributedLock getFairLock(String lockName) {
        // 数据库锁天然不支持公平语义，返回普通锁
        return getLock(lockName);
    }

    @Override
    public boolean tryLock(String lockName) {
        return acquireLock(lockName, -1L);
    }

    @Override
    public boolean tryLock(String lockName, long waitTime, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        do {
            if (acquireLock(lockName, -1L)) {
                return true;
            }
            sleepQuietly(50);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    @Override
    public void lock(String lockName) {
        DistributedLock lock = getLock(lockName);
        lock.lock();
    }

    @Override
    public void unlock(String lockName) {
        releaseLock(lockName);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
