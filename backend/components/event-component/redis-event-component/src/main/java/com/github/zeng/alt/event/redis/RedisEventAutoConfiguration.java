package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.support.EventCodec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 同时提供独立的 Streams 和 Pub/Sub 传输。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(
        after = EventAutoConfiguration.class,
        afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
)
public class RedisEventAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RedisStreamEventTemplate.class)
    public RedisStreamEventTemplate redisStreamEventTemplate(
            StringRedisTemplate redis,
            EventCodec codec,
            ObjectProvider<RedisEventCustomizer> customizers
    ) {
        return new RedisStreamEventTemplate(redis, codec, customizers);
    }

    @Bean
    @ConditionalOnMissingBean(RedisPubSubEventTemplate.class)
    public RedisPubSubEventTemplate redisPubSubEventTemplate(StringRedisTemplate redis, EventCodec codec) {
        return new RedisPubSubEventTemplate(redis, codec);
    }
}
