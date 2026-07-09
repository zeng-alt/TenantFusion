package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 数据库分布式锁执行器，基于 sys_distributed_lock 表实现
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@CommonsLog
public class DatabaseLockExecutor extends AbstractLockExecutor<String> {

    private final JdbcClient jdbcClient;
    private final String instanceId;

    public DatabaseLockExecutor(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        this.instanceId = UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        LocalDateTime expireAt = expire > 0
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(
                        System.currentTimeMillis() + expire), ZoneId.systemDefault())
                : null;

        // 1. 尝试插入新记录
        int inserted = jdbcClient.sql("""
                INSERT INTO sys_distributed_lock (lock_name, instance_id, locked_at, expire_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?)
                """)
                .params(lockKey, instanceId, expireAt)
                .update();

        if (inserted > 0) {
            log.debug(LogMessage.format("Lock acquired: key=%s, instance=%s", lockKey, instanceId));
            return instanceId;
        }

        // 2. 插入失败，检查是否已过期，尝试抢占
        int updated = jdbcClient.sql("""
                UPDATE sys_distributed_lock
                SET instance_id = ?, locked_at = CURRENT_TIMESTAMP, expire_at = ?
                WHERE lock_name = ? AND expire_at IS NOT NULL AND expire_at <= CURRENT_TIMESTAMP
                """)
                .params(instanceId, expireAt, lockKey)
                .update();

        if (updated > 0) {
            log.debug(LogMessage.format("Lock acquired (preempted): key=%s, instance=%s", lockKey, instanceId));
            return instanceId;
        }

        return null;
    }

    @Override
    public boolean releaseLock(String key, String value, String lockInstance) {
        int deleted = jdbcClient.sql(
                "DELETE FROM sys_distributed_lock WHERE lock_name = ? AND instance_id = ?")
                .params(key, lockInstance)
                .update();
        if (deleted > 0) {
            log.debug(LogMessage.format("Lock released: key=%s, instance=%s", key, lockInstance));
            return true;
        }
        return false;
    }
}
