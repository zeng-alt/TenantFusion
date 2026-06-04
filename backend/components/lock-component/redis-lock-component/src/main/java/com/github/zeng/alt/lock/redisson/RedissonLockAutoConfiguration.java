package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.LockTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 分布式锁自动配置
 * 当 classpath 中存在 RedissonClient 且未自定义 LockTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnMissingBean(LockTemplate.class)
public class RedissonLockAutoConfiguration {

    @Bean
    public LockTemplate redissonLockTemplate(RedissonClient redissonClient) {
        return new RedissonLockTemplate(redissonClient);
    }
}
