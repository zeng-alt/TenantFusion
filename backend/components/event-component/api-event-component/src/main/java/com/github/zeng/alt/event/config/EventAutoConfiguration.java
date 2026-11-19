package com.github.zeng.alt.event.config;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.subscription.EventSubscriptionAotProcessor;
import com.github.zeng.alt.event.subscription.EventSubscriptionTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventRuntimeHints;
import com.github.zeng.alt.event.support.EventTransportResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 装配统一事件发送与监听 API，传输实现按 binding 选择。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = EventContextAutoConfiguration.class)
@EnableConfigurationProperties(EventProperties.class)
@ImportRuntimeHints(EventRuntimeHints.class)
public class EventAutoConfiguration {
    @Bean
    public static EventSubscriptionAotProcessor eventSubscriptionAotProcessor() {
        return new EventSubscriptionAotProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventCodec eventCodec(ObjectProvider<ObjectMapper> mappers) {
        return new EventCodec(mappers.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build()));
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTransportResolver eventTransportResolver(
            EventProperties properties,
            ObjectProvider<EventTransportTemplate> transports
    ) {
        return new EventTransportResolver(properties, transports);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTemplate eventTemplate(
            EventTransportResolver transports,
            EventContextTemplate contexts,
            EventCodec codec
    ) {
        return new EventTemplate(transports, contexts, codec);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventSubscriptionTemplate eventSubscriptionTemplate(
            ConfigurableListableBeanFactory beans,
            EventTransportResolver transports,
            EventCodec codec,
            EventContextTemplate contexts
    ) {
        return new EventSubscriptionTemplate(beans, transports, codec, contexts);
    }
}
