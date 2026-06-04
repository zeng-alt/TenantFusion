package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import com.github.zeng.alt.storage.api.StorageTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Spring Cache 存储自动配置
 * 当 classpath 中存在 CacheManager 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnBean(CacheManager.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class SpringCacheStorageAutoConfiguration {

    @Bean
    public StorageTemplate springCacheStorageTemplate(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy) {
        return new SpringCacheStorageTemplate(cacheManager, keyPrefixStrategy);
    }
}
