package com.github.zeng.alt.storage.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储模块自动配置
 * 当没有注入任何 StorageTemplate 实现时，提供 NoOpStorageTemplate 作为默认 fallback
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@Configuration
public class StorageAutoConfiguration {

    /**
     * 默认提供 NoOpStorageTemplate 空实现
     * 当其他实现模块（如 redisson、spring-cache、jetcache）未引入时使用
     */
    @Bean
    @ConditionalOnMissingBean(StorageTemplate.class)
    public StorageTemplate noOpStorageTemplate() {
        return new NoOpStorageTemplate();
    }

    /**
     * 默认提供 KeyPrefixStrategy 空实现（不做前缀处理）
     */
    @Bean
    @ConditionalOnMissingBean(KeyPrefixStrategy.class)
    public KeyPrefixStrategy noOpKeyPrefixStrategy() {
        return KeyPrefixStrategy.noOp();
    }
}
