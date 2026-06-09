package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.model.LockUtils;
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

    boolean acquireLock(String lockName, long expireAtMillis) {
        LocalDateTime expireAt = expireAtMillis > 0
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expireAtMillis), ZoneId.systemDefault())
                : null;

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

    void releaseLock(String lockName) {
        jdbcClient.sql("DELETE FROM sys_distributed_lock WHERE lock_name = ? AND instance_id = ?")
                .params(lockName, instanceId)
                .update();
        lockHolders.remove(lockName);
    }

    boolean isHeldByCurrentThread(String lockName) {
        return Thread.currentThread().equals(lockHolders.get(lockName));
    }

    // ========== 函数式执行 ==========

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

    // ========== 锁管理 ==========

    @Override
    public DistributedLock getLock(String lockName) {
        return new DatabaseDistributedLock(this, lockName);
    }

    @Override
    public DistributedLock getFairLock(String lockName) {
        return getLock(lockName);
    }

    // ========== 直接操作 ==========

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

    @Override
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
    // ========== Lock4j 兼容 API ==========

    @Override
    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        String lockValue = LockUtils.simpleUUID();
        long expireAtMillis = expire > 0 ? System.currentTimeMillis() + expire : -1L;

        long deadline = System.currentTimeMillis() + acquireTimeout;
        int acquireCount = 0;
        do {
            acquireCount++;
            if (acquireLock(key, expireAtMillis)) {
                return new LockInfo(key, lockValue, expire, acquireTimeout, acquireCount, null, null);
            }
            sleepQuietly(50);
        } while (System.currentTimeMillis() < deadline);

        return null;
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }
        releaseLock(lockInfo.getLockKey());
        return true;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
