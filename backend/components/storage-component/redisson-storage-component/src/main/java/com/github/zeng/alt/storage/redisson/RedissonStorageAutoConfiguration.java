package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import com.github.zeng.alt.storage.api.StorageTemplate;
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
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class RedissonStorageAutoConfiguration {

    @Bean
    public StorageTemplate redissonStorageTemplate(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonStorageTemplate(redissonClient, keyPrefixStrategy);
    }
}
