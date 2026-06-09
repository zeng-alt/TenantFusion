package com.github.zeng.alt.lock.model;

import com.github.zeng.alt.lock.executor.LockExecutor;

import java.util.StringJoiner;

/**
 * 锁信息，保存一次成功加锁的完整上下文
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class LockInfo {

    /** 锁 key */
    private final String lockKey;

    /** 锁 value（用于安全解锁） */
    private final String lockValue;

    /** 过期时间（毫秒） */
    private final long expire;

    /** 获取锁超时时间（毫秒） */
    private final long acquireTimeout;

    /** 获取锁尝试次数 */
    private final int acquireCount;

    /** 底层锁实例 */
    private final Object lockInstance;

    /** 锁执行器 */
    private final LockExecutor<?> lockExecutor;

    public LockInfo(String lockKey, String lockValue, long expire, long acquireTimeout,
                    int acquireCount, Object lockInstance, LockExecutor<?> lockExecutor) {
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expire = expire;
        this.acquireTimeout = acquireTimeout;
        this.acquireCount = acquireCount;
        this.lockInstance = lockInstance;
        this.lockExecutor = lockExecutor;
    }

    public String getLockKey() {
        return lockKey;
    }

    public String getLockValue() {
        return lockValue;
    }

    public long getExpire() {
        return expire;
    }

    public long getAcquireTimeout() {
        return acquireTimeout;
    }

    public int getAcquireCount() {
        return acquireCount;
    }

    public Object getLockInstance() {
        return lockInstance;
    }

    public LockExecutor<?> getLockExecutor() {
        return lockExecutor;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LockInfo.class.getSimpleName() + "[", "]")
                .add("lockKey='" + lockKey + "'")
                .add("expire=" + expire)
                .add("acquireTimeout=" + acquireTimeout)
                .add("acquireCount=" + acquireCount)
                .toString();
    }
}
