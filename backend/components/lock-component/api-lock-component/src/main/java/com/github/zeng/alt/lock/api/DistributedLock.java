package com.github.zeng.alt.lock.api;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口，支持 try-with-resources 语法
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface DistributedLock extends AutoCloseable {

    /**
     * 获取锁名称
     *
     * @return 锁名称
     */
    String getName();

    /**
     * 尝试获取锁，立即返回
     *
     * @return true 获取成功
     */
    boolean tryLock();

    /**
     * 尝试获取锁，等待指定时间
     *
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @return true 获取成功
     */
    boolean tryLock(long waitTime, TimeUnit unit);

    /**
     * 尝试获取锁，等待指定时间，持有指定时间
     *
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间（自动释放）
     * @param unit      时间单位
     * @return true 获取成功
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 阻塞直到获取锁
     */
    void lock();

    /**
     * 释放锁
     */
    void unlock();

    /**
     * 当前线程是否持有此锁
     *
     * @return true 持有
     */
    boolean isHeldByCurrentThread();

    /**
     * 锁是否被任何线程持有
     *
     * @return true 已锁定
     */
    boolean isLocked();

    @Override
    default void close() {
        unlock();
    }
}
