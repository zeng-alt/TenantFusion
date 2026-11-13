package com.github.zeng.alt.message.redis;

import com.github.zeng.alt.message.MessageQueueTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Redis 消息组件自动配置。
 * <p>
 * Redis message component auto-configuration.
 * <p>
 * 当 classpath 中存在 {@link RedissonClient} 时自动装配
 * {@link RedisMessageQueueTemplate}。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
public class RedisMessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageQueueTemplate.class)
    public MessageQueueTemplate redisMessageQueueTemplate(RedissonClient redissonClient) {
        return new RedisMessageQueueTemplate(redissonClient);
    }
}
