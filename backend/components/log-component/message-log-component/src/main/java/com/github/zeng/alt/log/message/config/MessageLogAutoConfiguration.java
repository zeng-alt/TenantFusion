package com.github.zeng.alt.log.message.config;

import com.github.zeng.alt.log.message.consumer.MessageLogConsumer;
import com.github.zeng.alt.log.message.producer.MessageLogProducer;
import com.github.zeng.alt.event.EventTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 消息日志模块自动配置。
 * <p>
 * Message log module auto-configuration.
 * <p>
 * 在多服务部署时，日志通过消息队列进行跨服务传递：
 * <ul>
 *   <li><b>生产者</b> — 监听本地 {@code OperLogEvent}，发送到消息队列</li>
 *   <li><b>消费者</b> — 从消息队列接收日志，重新发布到本地 ApplicationContext</li>
 * </ul>
 * <p>
 * 生产者和消费者均可通过配置独立控制启停。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnBean(EventTemplate.class)
public class MessageLogAutoConfiguration {

    /**
     * 消息日志生产者：将本地操作日志发送到消息队列。
     * <p>
     * 默认启用，通过 {@code log.message.producer-enabled=false} 关闭。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "log.message.producer-enabled", havingValue = "true", matchIfMissing = true)
    public MessageLogProducer messageLogProducer(EventTemplate messageQueue) {
        return new MessageLogProducer(messageQueue);
    }

    /**
     * 消息日志消费者：从消息队列接收日志并重新发布到应用上下文。
     * <p>
     * 默认关闭（仅生产者模式），通过 {@code log.message.consumer-enabled=true} 开启。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "log.message.consumer-enabled", havingValue = "true")
    public MessageLogConsumer messageLogConsumer(ApplicationEventPublisher eventPublisher) {
        return new MessageLogConsumer(eventPublisher);
    }

}
