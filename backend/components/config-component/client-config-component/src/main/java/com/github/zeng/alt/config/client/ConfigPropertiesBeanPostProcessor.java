package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.annotation.ConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 为自动注册的配置类按属性前缀绑定字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigPropertiesBeanPostProcessor implements BeanPostProcessor {
    private final ConfigurableListableBeanFactory beans;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        if (!beans.containsBeanDefinition(name) || !"businessConfig".equals(beans.getBeanDefinition(name).getScope())) {
            return bean;
        }
        final ConfigProperties binding = beans.findAnnotationOnBean(name, ConfigProperties.class, false);
        if (binding != null) {
            beans.getBean(ConfigPropertiesBindingTemplate.class).bind(bean, binding);
        }
        return bean;
    }
}
