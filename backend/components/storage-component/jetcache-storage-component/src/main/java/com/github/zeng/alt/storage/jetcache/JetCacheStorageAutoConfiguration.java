package com.github.zeng.alt.storage.jetcache;

import com.alicp.jetcache.CacheManager;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import com.github.zeng.alt.storage.api.StorageAutoConfiguration;
import com.github.zeng.alt.storage.api.StorageTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * JetCache 存储自动配置
 * 当 classpath 中存在 CacheManager 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration(before = StorageAutoConfiguration.class)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class JetCacheStorageAutoConfiguration {

    @Bean
    public StorageTemplate jetCacheStorageTemplate(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        return new JetCacheStorageTemplate(cacheManager, keyPrefixStrategy, lockTemplate);
    }

}
