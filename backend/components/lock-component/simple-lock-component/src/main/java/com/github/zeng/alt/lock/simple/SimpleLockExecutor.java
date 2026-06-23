package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;

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

    private final Log log = LogFactory.getLog(SimpleLockExecutor.class);

    private final ReentrantLock lock;

    public SimpleLockExecutor() {
        this.lock = new ReentrantLock();
    }

    @Override
    public ReentrantLock acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        try {
            if (acquireTimeout > 0) {
                boolean locked = lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS);
                if (locked) {
                    log.debug(LogMessage.format("Lock acquired: key=%s, value=%s", lockKey, lockValue));
                    return lock;
                }
            } else {
                lock.lock();
                log.debug(LogMessage.format("Lock acquired (blocking): key=%s, value=%s", lockKey, lockValue));
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(LogMessage.format("Lock acquisition interrupted: key=%s", lockKey), e);
        }
        return null;
    }

    @Override
    public boolean releaseLock(String key, String value, ReentrantLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            log.debug(LogMessage.format("Lock released: key=%s, value=%s", key, value));
            return true;
        }
        return false;
    }
}
