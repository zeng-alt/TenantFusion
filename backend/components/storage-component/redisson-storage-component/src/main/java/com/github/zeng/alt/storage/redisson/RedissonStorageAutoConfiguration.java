package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.storage.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 存储自动配置
 * 当 classpath 中存在 RedissonClient 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration(before = StorageAutoConfiguration.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class RedissonStorageAutoConfiguration {





    @Bean
    public StorageTemplate redissonStorageTemplate(
            RedissonClient redissonClient,
            KeyPrefixStrategy keyPrefixStrategy,
            CacheStringOperations cacheStringOperations,
            CacheListOperations cacheListOperations,
            CacheHashOperations cacheHashOperations,
            CacheZSetOperations cacheZSetOperations
    ) {
        return new RedissonStorageTemplate(
                redissonClient, keyPrefixStrategy,
                cacheStringOperations,
                cacheListOperations,
                cacheHashOperations,
                cacheZSetOperations
        );
    }

    @Bean
    public CacheStringOperations noOpCacheStringOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonStringOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheListOperations noOpCacheListOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonListOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheHashOperations noOpCacheHashOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonHashOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheZSetOperations noOpCacheZSetOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonZSetOperations(redissonClient, keyPrefixStrategy);
    }
}
