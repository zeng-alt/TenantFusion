package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import com.github.zeng.alt.storage.api.StorageTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring Data Redis 存储自动配置
 * 当 classpath 中存在 StringRedisTemplate 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class SpringCacheStorageAutoConfiguration {

    @Bean
    public StorageTemplate springCacheStorageTemplate(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        return new SpringCacheStorageTemplate(redisTemplate, keyPrefixStrategy);
    }
}
