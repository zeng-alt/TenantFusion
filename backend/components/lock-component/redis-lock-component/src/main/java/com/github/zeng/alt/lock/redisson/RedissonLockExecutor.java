package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import lombok.extern.apachecommons.CommonsLog;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.log.LogMessage;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁执行器
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@CommonsLog
public class RedissonLockExecutor extends AbstractLockExecutor<RLock> {

    private final RedissonClient redissonClient;

    public RedissonLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean renewal() {
        return true;
    }

    @Override
    public RLock acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (acquireTimeout > 0) {
                // expire <= 0 交由 Redisson 的看门狗机制续期
                long waitTime = acquireTimeout;
                long leaseTime = expire > 0 ? expire : -1;
                boolean locked;
                if (leaseTime > 0) {
                    locked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
                } else {
                    locked = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
                }
                if (locked) {
                    log.debug(LogMessage.format("Lock acquired: key=%s, expire=%sms", lockKey, expire));
                    return lock;
                }
            } else {
                lock.lock();
                log.debug(LogMessage.format("Lock acquired (blocking): key=%s", lockKey));
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(LogMessage.format("Lock acquisition interrupted: key=%s", lockKey, e));
        }
        return null;
    }

    @Override
    public boolean releaseLock(String key, String value, RLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            log.debug(LogMessage.format("Lock released: key=%s", key));
            return true;
        }
        return false;
    }
}
