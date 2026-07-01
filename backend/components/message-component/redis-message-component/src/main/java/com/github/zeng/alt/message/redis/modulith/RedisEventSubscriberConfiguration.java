package com.github.zeng.alt.message.redis.modulith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Redis Spring Modulith Events 订阅端配置。
 * <p>
 * Redis event subscriber auto-configuration.
 * <p>
 * 监听 {@code modulith:event:*} 频道模式，接收外部化的领域事件，
 * 反序列化后重新发布到当前应用的 {@link ApplicationEventPublisher}。
 * <p>
 * 这样，跨服务的事件可以通过 Redis Pub/Sub 进行广播消费。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass({RedisConnectionFactory.class, ApplicationEventPublisher.class})
public class RedisEventSubscriberConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisEventSubscriberConfiguration.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    @ConditionalOnMissingBean(name = "redisModulithEventContainer")
    public RedisMessageListenerContainer redisModulithEventContainer(
            RedisConnectionFactory connectionFactory,
            ApplicationEventPublisher eventPublisher) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                new RedisEventMessageListener(eventPublisher),
                new PatternTopic("modulith:event:*")
        );

        container.start();
        log.info("Redis Modulith Event subscriber started, listening on 'modulith:event:*'");
        return container;
    }

    /**
     * Redis 消息监听器：将接收到的外部化事件反序列化并重新发布。
     */
    private static class RedisEventMessageListener implements MessageListener {

        private final ApplicationEventPublisher eventPublisher;

        RedisEventMessageListener(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = RedisSerializer.string().deserialize(message.getBody());
                if (body == null || body.isBlank()) {
                    return;
                }

                // 解析 JSON 信封
                JsonNode root = OBJECT_MAPPER.readTree(body);
                JsonNode eventTypeNode = root.get("@eventType");
                JsonNode dataNode = root.get("@data");

                if (eventTypeNode == null || dataNode == null) {
                    log.warn("Received Redis event message missing @eventType or @data, skipping");
                    return;
                }

                // 加载事件类型并反序列化
                String eventTypeName = eventTypeNode.asText();
                Class<?> eventType = Class.forName(eventTypeName);
                Object event = OBJECT_MAPPER.treeToValue(dataNode, eventType);

                // 重新发布到 ApplicationContext
                eventPublisher.publishEvent(event);

                log.debug("Redis Modulith event re-published: type={}", eventTypeName);
            } catch (Exception e) {
                log.error("Failed to process incoming Redis Modulith event", e);
            }
        }
    }
}
