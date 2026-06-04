package com.github.zeng.alt.lock.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 锁模块自动配置
 * 当没有注入任何 LockTemplate 实现时，提供 NoOpLockTemplate 作为默认 fallback
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@Configuration
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate noOpLockTemplate() {
        return new NoOpLockTemplate();
    }
}
