package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁执行器
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class RedissonLockExecutor extends AbstractLockExecutor<RLock> {

    private static final Logger log = LoggerFactory.getLogger(RedissonLockExecutor.class);

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
                    log.debug("Lock acquired: key={}, expire={}ms", lockKey, expire);
                    return lock;
                }
            } else {
                lock.lock();
                log.debug("Lock acquired (blocking): key={}", lockKey);
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted: key={}", lockKey, e);
        }
        return null;
    }

    @Override
    public boolean releaseLock(String key, String value, RLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            log.debug("Lock released: key={}", key);
            return true;
        }
        return false;
    }
}
