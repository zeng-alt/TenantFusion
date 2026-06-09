package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Simple 本地锁自动配置
 * 提供本地内存锁模板和执行器（适用于开发/测试或单机环境）
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
public class SimpleLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockExecutor<?> simpleLockExecutor() {
        return new SimpleLockExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate simpleLockTemplate() {
        return new SimpleLockTemplate();
    }
}
