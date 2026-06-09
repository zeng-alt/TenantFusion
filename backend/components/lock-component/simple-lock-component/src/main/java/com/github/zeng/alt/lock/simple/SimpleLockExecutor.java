package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地内存锁执行器（JVM 级别，适用于单机环境）
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class SimpleLockExecutor extends AbstractLockExecutor<ReentrantLock> {

    private static final Logger log = LoggerFactory.getLogger(SimpleLockExecutor.class);

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public ReentrantLock acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        try {
            if (acquireTimeout > 0) {
                boolean locked = lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS);
                if (locked) {
                    log.debug("Lock acquired: key={}, value={}", lockKey, lockValue);
                    return lock;
                }
            } else {
                lock.lock();
                log.debug("Lock acquired (blocking): key={}, value={}", lockKey, lockValue);
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted: key={}", lockKey, e);
        }
        return null;
    }

    @Override
    public boolean releaseLock(String key, String value, ReentrantLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            log.debug("Lock released: key={}, value={}", key, value);
            return true;
        }
        return false;
    }
}
