package com.github.zeng.alt.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 存储模块自动配置
 * 当没有注入任何 StorageTemplate 实现时，提供 NoOpStorageTemplate 作为默认 fallback
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties({StorageProperties.class})
public class StorageAutoConfiguration {

    /**
     * 默认提供 NoOpStorageTemplate 空实现
     * 当其他实现模块（如 redisson、spring-cache、jetcache）未引入时使用
     */
    @Bean
    @ConditionalOnMissingBean(StorageTemplate.class)
    public StorageTemplate noOpStorageTemplate(
            CacheStringOperations cacheStringOperations,
            CacheListOperations cacheListOperations,
            CacheHashOperations cacheHashOperations,
            CacheZSetOperations cacheZSetOperations
    ) {
        return new NoOpStorageTemplate(cacheStringOperations, cacheListOperations, cacheHashOperations, cacheZSetOperations);
    }


    @Bean
    @ConditionalOnMissingBean(CacheStringOperations.class)
    public CacheStringOperations noOpCacheStringOperations() {
        return new GuavaCacheStringOperations();
    }

    @Bean
    @ConditionalOnMissingBean(CacheListOperations.class)
    public CacheListOperations noOpCacheListOperations() {
        return new NoOpCacheListOperations();
    }

    @Bean
    @ConditionalOnMissingBean(CacheHashOperations.class)
    public CacheHashOperations noOpCacheHashOperations() {
        return new NoOpCacheHashOperations();
    }

    @Bean
    @ConditionalOnMissingBean(CacheZSetOperations.class)
    public CacheZSetOperations noOpCacheZSetOperations() {
        return new NoOpCacheZSetOperations();
    }

    /**
     * 默认提供 KeyPrefixStrategy 空实现（不做前缀处理）
     */
    @Bean
    @Primary
    public KeyPrefixStrategy defaultKeyPrefixStrategy(ObjectProvider<KeyPrefixStrategy> prefixStrategies) {
        return prefixStrategies.orderedStream()
                .reduce(
                        KeyPrefixStrategy.noOp(),
                        KeyPrefixStrategy::andThen
                );
    }
}
