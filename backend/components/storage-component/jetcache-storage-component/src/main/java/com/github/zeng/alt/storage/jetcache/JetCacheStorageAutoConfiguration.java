package com.github.zeng.alt.storage.jetcache;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.StorageAutoConfiguration;
import com.github.zeng.alt.storage.StorageProperties;
import com.github.zeng.alt.storage.StorageTemplate;
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
    public StorageTemplate jetCacheStorageTemplate(CacheManager cacheManager, StorageProperties storageProperties, KeyPrefixStrategy keyPrefixStrategy, CacheStringOperations cacheStringOperations) {
        return new JetCacheStorageTemplate(createJetCache(cacheManager, storageProperties), keyPrefixStrategy, cacheStringOperations);
    }

    @Bean
    public CacheStringOperations jetCacheStringOperations(CacheManager cacheManager, StorageProperties storageProperties, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        return new JetCacheStringOperations(createJetCache(cacheManager, storageProperties), keyPrefixStrategy, lockTemplate);
    }

    public Cache<String, Object> createJetCache(CacheManager cacheManager, StorageProperties storageProperties) {
        QuickConfig qc = QuickConfig.newBuilder("storage:")
                .cacheType(CacheType.BOTH)
                .expire(storageProperties.getExpireTime())
                .cacheNullValue(storageProperties.getAllowNullValues())
                .build();
        return cacheManager.getOrCreateCache(qc);
    }

}
