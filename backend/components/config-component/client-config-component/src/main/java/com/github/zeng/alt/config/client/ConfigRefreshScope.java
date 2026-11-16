package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.annotation.ConfigBean;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import com.github.zeng.alt.config.annotation.ConfigValue;
import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 业务配置专用作用域，复用 Spring Cloud 的代理锁和销毁回调。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class ConfigRefreshScope extends GenericScope implements ApplicationListener<ConfigChangeEvent>, Ordered {
    private final ObjectProvider<ConfigClientProperties> properties;
    private final Map<ConfigChangedKey, Set<String>> beansByKey = new HashMap<>();
    private final Map<String, ConfigProperties> propertiesByBean = new HashMap<>();

    public ConfigRefreshScope(ObjectProvider<ConfigClientProperties> properties) {
        this.properties = properties;
        setName("businessConfig");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beans) {
        super.postProcessBeanFactory(beans);
        Arrays.stream(beans.getBeanDefinitionNames())
                .filter(name -> getName().equals(beans.getBeanDefinition(name).getScope()))
                .forEach(name -> register(beans, name));
    }

    private void register(ConfigurableListableBeanFactory beans, String name) {
        final ConfigProperties configuration = beans.findAnnotationOnBean(name, ConfigProperties.class, false);
        if (configuration != null) {
            ConfigurationPropertyName.of(configuration.prefix());
            propertiesByBean.put(name, configuration);
            return;
        }
        final ConfigBean binding = beans.findAnnotationOnBean(name, ConfigBean.class, false);
        if (binding == null) {
            return;
        }
        final Set<ConfigChangedKey> dependencies = new HashSet<>();
        Arrays.stream(binding.keys()).forEach(key -> dependencies.add(new ConfigChangedKey(key, binding.group())));
        final Class<?> type = beans.getType(name, false);
        if (type != null) {
            ReflectionUtils.doWithFields(
                type,
                field -> {
                    final ConfigValue value = field.getAnnotation(ConfigValue.class);
                    if (value != null) {
                        dependencies.add(new ConfigChangedKey(
                            value.value(),
                            StringUtils.hasText(value.group()) ? value.group() : binding.group()
                        ));
                    }
                }
            );
        }
        if (dependencies.isEmpty()) {
            throw new ConfigException("ConfigBean 必须声明 keys 或 ConfigValue 字段: " + name);
        }
        dependencies.forEach(key -> beansByKey.computeIfAbsent(key, ignored -> new HashSet<>()).add(name));
    }

    @Override
    public void onApplicationEvent(ConfigChangeEvent event) {
        if (!properties.getObject().getAppCode().equals(event.getAppCode())) {
            return;
        }
        java.util.stream.Stream.concat(
            event.getChangedKeys().stream().flatMap(key -> beansByKey.getOrDefault(key, Set.of()).stream()),
            propertiesByBean.entrySet().stream().filter(entry -> event.getChangedKeys().stream()
                    .anyMatch(key -> matches(entry.getValue(), key))).map(Map.Entry::getKey)
        ).distinct().forEach(this::destroy);
    }
    private boolean matches(ConfigProperties binding, ConfigChangedKey key) {
        if (!binding.group().equals(key.getGroup())) {
            return false;
        }
        return ConfigPropertiesHelper.matches(binding.prefix(), key.getDataId());
    }
}
