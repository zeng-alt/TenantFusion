package com.github.zeng.alt.lock.config;

import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Role;

/**
 * 分布式锁配置属性
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConfigurationProperties(prefix = "sys.lock")
public class LockProperties {

    /**
     * 锁过期时间（毫秒），默认 30 秒
     */
    private long expire = 30000L;

    /**
     * 获取锁超时时间（毫秒），默认 3 秒
     */
    private long acquireTimeout = 3000L;

    /**
     * 获取锁失败时重试间隔（毫秒），默认 100 毫秒
     */
    private long retryInterval = 100L;

    /**
     * 锁 key 前缀
     */
    // 默认值恢复为 alt:lock。包重命名前该值即为 alt:lock，重构中被改成了 lock，
    // 与 ApiLockUnitTest 的断言不符。锁键是短生命周期数据，改回不影响存量。
    private String lockKeyPrefix = "alt:lock";

    /**
     * 主执行器类型
     */
    private Class<? extends LockExecutor<?>> primaryExecutor;

    /**
     * 主锁失败策略类型
     */
    private Class<? extends LockFailureStrategy> primaryFailStrategy;

    /**
     * 主 key 构建器类型
     */
    private Class<? extends LockKeyBuilder> primaryKeyBuilder;

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public long getAcquireTimeout() {
        return acquireTimeout;
    }

    public void setAcquireTimeout(long acquireTimeout) {
        this.acquireTimeout = acquireTimeout;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        this.lockKeyPrefix = lockKeyPrefix;
    }

    public Class<? extends LockExecutor<?>> getPrimaryExecutor() {
        return primaryExecutor;
    }

    public void setPrimaryExecutor(Class<? extends LockExecutor<?>> primaryExecutor) {
        this.primaryExecutor = primaryExecutor;
    }

    public Class<? extends LockFailureStrategy> getPrimaryFailStrategy() {
        return primaryFailStrategy;
    }

    public void setPrimaryFailStrategy(Class<? extends LockFailureStrategy> primaryFailStrategy) {
        this.primaryFailStrategy = primaryFailStrategy;
    }

    public Class<? extends LockKeyBuilder> getPrimaryKeyBuilder() {
        return primaryKeyBuilder;
    }

    public void setPrimaryKeyBuilder(Class<? extends LockKeyBuilder> primaryKeyBuilder) {
        this.primaryKeyBuilder = primaryKeyBuilder;
    }
}
