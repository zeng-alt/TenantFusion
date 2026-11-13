package com.github.zeng.alt.event.config;

import com.github.zeng.alt.event.context.EventContextHandler;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.context.RequestEventContextHandler;
import com.github.zeng.alt.event.internal.EventContextHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;

/**
 * 装配与发布框架无关的上下文 SPI、作用域和快照关联。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@Import(SpringEventContextConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class EventContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestEventContextHandler requestEventContextHandler() {
        return new RequestEventContextHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public EventContextTemplate eventContextTemplate(ObjectProvider<EventContextHandler> handlers) {
        return new EventContextTemplate(handlers);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public EventContextHelper eventContextHelper(EventContextTemplate contexts) {
        return new EventContextHelper(contexts);
    }
}
