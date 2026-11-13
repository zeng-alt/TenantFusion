package com.github.zeng.alt.event.config;

import com.github.zeng.alt.event.internal.ContextEventSerializer;
import com.github.zeng.alt.event.internal.EventContextHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.modulith.events.core.EventSerializer;

/**
 * 通过 Modulith 官方 EventSerializer 扩展点装饰序列化，不替换事件广播器或业务事件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = EventContextAutoConfiguration.class)
@ConditionalOnClass(EventSerializer.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ModulithEventContextAutoConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanPostProcessor contextEventSerializerPostProcessor(EventContextHelper snapshots) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof EventSerializer serializer && !(bean instanceof ContextEventSerializer)) {
                    return new ContextEventSerializer(serializer, snapshots);
                }
                return bean;
            }
        };
    }
}
