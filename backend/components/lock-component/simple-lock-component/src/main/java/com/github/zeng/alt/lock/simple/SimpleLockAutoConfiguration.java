package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.LockTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Simple 本地锁自动配置
 * 当未自定义 LockTemplate 时作为默认锁实现（适用于开发/测试环境）
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnMissingBean(LockTemplate.class)
public class SimpleLockAutoConfiguration {

    @Bean
    public LockTemplate simpleLockTemplate() {
        return new SimpleLockTemplate();
    }
}
