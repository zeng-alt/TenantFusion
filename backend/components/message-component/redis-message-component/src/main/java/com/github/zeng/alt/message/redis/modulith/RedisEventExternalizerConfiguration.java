package com.github.zeng.alt.message.redis.modulith;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.support.DelegatingEventExternalizer;

import java.util.concurrent.CompletableFuture;

/**
 * Redis Spring Modulith Events 外部化配置。
 * <p>
 * Redis event externalizer auto-configuration.
 * <p>
 * 当应用使用 {@code ApplicationEventPublisher} 发布事件时，如果事件满足
 * {@link EventExternalizationConfiguration} 的路由规则（如标注了
 * {@code @Externalized}），则通过 Redis Pub/Sub 将事件发送到 Redis。
 * <p>
 * 事件以 JSON 格式发布到 {@code modulith:event:<routingTarget>} 频道。
 * 负载的类名自动写入 JSON 中的 {@code @eventType} 字段，供消费者反序列化。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass({StringRedisTemplate.class, DelegatingEventExternalizer.class})
public class RedisEventExternalizerConfiguration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHANNEL_PREFIX = "modulith:event:";

    @Bean
    @ConditionalOnMissingBean(DelegatingEventExternalizer.class)
    public DelegatingEventExternalizer redisEventExternalizer(
            EventExternalizationConfiguration configuration,
            StringRedisTemplate redisTemplate) {

        return new DelegatingEventExternalizer(configuration, (target, payload) -> {
            try {
                String channel = CHANNEL_PREFIX + target.getTarget();

                // 将事件序列化为 JSON，并注入类型信息
                String payloadJson = OBJECT_MAPPER.writeValueAsString(payload);
                String message = "{\"@eventType\":\"" + payload.getClass().getName()
                        + "\",\"@data\":" + payloadJson + "}";

                redisTemplate.convertAndSend(channel, message);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    /**
     * Redis 事件频道解析工具。从完整频道名 {@code modulith:event:<target>}
     * 中提取路由目标。
     */
    static String resolveTargetFromChannel(String channel) {
        if (channel != null && channel.startsWith(CHANNEL_PREFIX)) {
            return channel.substring(CHANNEL_PREFIX.length());
        }
        return channel;
    }
}
