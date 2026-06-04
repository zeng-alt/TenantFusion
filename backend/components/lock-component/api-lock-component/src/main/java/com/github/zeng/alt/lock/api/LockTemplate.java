package com.github.zeng.alt.lock.api;

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

    /**
     * 获取锁后执行，自动释放锁（默认一直等待）
     *
     * @param lockName 锁名称
     * @param supplier 执行逻辑
     * @param <T>      返回值类型
     * @return 执行结果
     */
    <T> T execute(String lockName, Supplier<T> supplier);

    /**
     * 获取锁后执行，自动释放锁（默认一直等待）
     *
     * @param lockName 锁名称
     * @param runnable 执行逻辑
     */
    void execute(String lockName, Runnable runnable);

    /**
     * 尝试获取锁后执行，自动释放锁
     *
     * @param lockName  锁名称
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间
     * @param unit      时间单位
     * @param supplier  执行逻辑
     * @param <T>       返回值类型
     * @return 执行结果（获取锁失败返回 null）
     */
    <T> T execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier);

    /**
     * 尝试获取锁后执行，自动释放锁
     *
     * @param lockName  锁名称
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间
     * @param unit      时间单位
     * @param runnable  执行逻辑
     */
    void execute(String lockName, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable);

    // ========== 锁管理 ==========

    /**
     * 获取可重入锁
     *
     * @param lockName 锁名称
     * @return DistributedLock
     */
    DistributedLock getLock(String lockName);

    /**
     * 获取公平锁
     *
     * @param lockName 锁名称
     * @return DistributedLock
     */
    DistributedLock getFairLock(String lockName);

    // ========== 直接操作 ==========

    /**
     * 尝试获取锁
     *
     * @param lockName 锁名称
     * @return true 获取成功
     */
    boolean tryLock(String lockName);

    /**
     * 尝试获取锁
     *
     * @param lockName 锁名称
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @return true 获取成功
     */
    boolean tryLock(String lockName, long waitTime, TimeUnit unit);

    /**
     * 阻塞直到获取锁
     *
     * @param lockName 锁名称
     */
    void lock(String lockName);

    /**
     * 释放锁
     *
     * @param lockName 锁名称
     */
    void unlock(String lockName);

    /**
     * 锁是否被持有
     *
     * @param lockName 锁名称
     * @return true 已锁定
     */
    boolean isLocked(String lockName);
}
