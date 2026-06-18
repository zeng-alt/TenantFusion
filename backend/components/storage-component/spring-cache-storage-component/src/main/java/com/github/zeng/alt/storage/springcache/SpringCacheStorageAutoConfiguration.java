package com.github.zeng.alt.storage.springcache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Spring Cache 存储自动配置
 * 当 classpath 中存在 CacheManager 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration(before = StorageAutoConfiguration.class)
@ConditionalOnBean(CacheManager.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class SpringCacheStorageAutoConfiguration {

    private static final String SPEC = "initialCapacity=100,maximumSize=500,expireAfterAccess=5m,recordStats";

    @Bean
    @Primary
    @ConditionalOnMissingBean(CaffeineCacheManager.class)
    public CaffeineCacheManager caffeineCacheManager(CacheProperties cacheProperties, ObjectProvider<CacheManagerCustomizer<CaffeineCacheManager>> customizers,
                                                     ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
                                                     ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
        CaffeineCacheManager cacheManager = createCacheManager(cacheProperties, caffeine, caffeineSpec, cacheLoader);
        if (CacheType.CAFFEINE.equals(cacheProperties.getType())) {
            List<String> cacheNames = cacheProperties.getCacheNames();
            if (!CollectionUtils.isEmpty(cacheNames)) {
                cacheManager.setCacheNames(cacheNames);
            }
        }
        customizers.forEach(customizer -> customizer.customize(cacheManager));
        return cacheManager;
    }

    private CaffeineCacheManager createCacheManager(CacheProperties cacheProperties,
                                                    ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
                                                    ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        setCacheBuilder(cacheProperties, caffeineSpec.getIfAvailable(), caffeine.getIfAvailable(), cacheManager);
        cacheLoader.ifAvailable(cacheManager::setCacheLoader);
        return cacheManager;
    }

    private void setCacheBuilder(CacheProperties cacheProperties, CaffeineSpec caffeineSpec,
                                 Caffeine<Object, Object> caffeine, CaffeineCacheManager cacheManager) {
        String specification = cacheProperties.getCaffeine().getSpec();
        cacheManager.setCacheSpecification(SPEC);

        if (StringUtils.hasText(specification)) {
            cacheManager.setCacheSpecification(specification);
        }
        else if (caffeineSpec != null) {
            cacheManager.setCaffeineSpec(caffeineSpec);
        }
        else if (caffeine != null) {
            cacheManager.setCaffeine(caffeine);
        }
    }

    @Bean
    public StorageTemplate springCacheStorageTemplate(org.springframework.cache.Cache storageCache, KeyPrefixStrategy keyPrefixStrategy, CacheStringOperations cacheStringOperations) {
        return new SpringCacheStorageTemplate(storageCache, keyPrefixStrategy, cacheStringOperations);
    }

    @Bean
    public CacheStringOperations cacheStringOperations(org.springframework.cache.Cache storageCache, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        return new SpringCacheStringOperations(storageCache, keyPrefixStrategy, lockTemplate);
    }

    @Bean
    public org.springframework.cache.Cache storageCache(StorageProperties storageProperties) {
        Caffeine<Object, Object> build = Caffeine.newBuilder()
                .initialCapacity(storageProperties.getInitialCapacity())
                .maximumSize(storageProperties.getMaximumSize())
                .expireAfterWrite(storageProperties.getExpireTime());
        if (storageProperties.getRecordStats()) {
            build.recordStats();
        }
        return
                new CaffeineCache("storage", build.build(), storageProperties.getAllowNullValues());
    }
}
