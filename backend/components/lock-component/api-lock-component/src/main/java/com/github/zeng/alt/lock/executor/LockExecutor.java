package com.github.zeng.alt.lock.executor;

/**
 * 分布式锁执行器 SPI，用于对接不同的锁后端
 *
 * @param <T> 锁实例类型
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public interface LockExecutor<T> {

    /**
     * 是否支持续期（目前仅 Redisson 支持）
     *
     * @return true 支持续期
     */
    default boolean renewal() {
        return false;
    }

    /**
     * 尝试加锁
     *
     * @param lockKey        锁 key
     * @param lockValue      锁 value（用于安全解锁）
     * @param expire         锁过期时间（毫秒），小于等于 0 表示永不过期
     * @param acquireTimeout 获取锁超时时间（毫秒）
     * @return 成功返回锁实例，失败返回 null
     */
    T acquire(String lockKey, String lockValue, long expire, long acquireTimeout);

    /**
     * 解锁
     *
     * @param key          锁 key
     * @param value        锁 value（校验身份，防止解错锁）
     * @param lockInstance 锁实例（acquire 的返回值）
     * @return true 释放成功
     */
    boolean releaseLock(String key, String value, T lockInstance);
}
