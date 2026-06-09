package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 分布式锁自动配置
 * 提供基于 RedissonClient 的分布式锁模板和执行器
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
public class RedissonLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockExecutor<?> redissonLockExecutor(RedissonClient redissonClient) {
        return new RedissonLockExecutor(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate redissonLockTemplate(RedissonClient redissonClient) {
        return new RedissonLockTemplate(redissonClient);
    }
}
