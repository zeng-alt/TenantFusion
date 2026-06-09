package com.github.zeng.alt.lock.api;

import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockInfo;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁模板，提供函数式执行和锁管理能力
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface LockTemplate {

    // ========== 函数式执行 ==========

    <T> T execute(String lockName, Supplier<T> supplier);

    void execute(String lockName, Runnable runnable);

    <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier);

    void execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable);

    // ========== 锁管理 ==========

    DistributedLock getLock(String lockName);

    DistributedLock getFairLock(String lockName);

    // ========== 直接操作 ==========

    boolean tryLock(String lockName);

    boolean tryLock(String lockName, long waitTime, TimeUnit unit);

    void lock(String lockName);

    void unlock(String lockName);

    boolean isLocked(String lockName);

    // ========== Lock4j 兼容 API ==========

    /**
     * 获取锁，默认使用主执行器
     *
     * @param key            锁 key
     * @param expire         锁过期时间（毫秒）
     * @param acquireTimeout 获取锁超时时间（毫秒）
     * @return 锁信息，失败返回 null
     */
    default LockInfo lock(String key, long expire, long acquireTimeout) {
        return lock(key, expire, acquireTimeout, null);
    }

    /**
     * 获取锁，指定执行器
     *
     * @param key            锁 key
     * @param expire         锁过期时间（毫秒）
     * @param acquireTimeout 获取锁超时时间（毫秒）
     * @param executor       锁执行器类型
     * @return 锁信息，失败返回 null
     */
    LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor);

    /**
     * 释放锁
     *
     * @param lockInfo 锁信息
     * @return true 释放成功
     */
    boolean releaseLock(LockInfo lockInfo);
}
