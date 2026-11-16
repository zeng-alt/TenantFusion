package com.github.zeng.alt.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.config.ConfigObjectTemplate;
import com.github.zeng.alt.config.ConfigSubscriptionHandler;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.ConfigValueTemplate;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

/**
 * 按应用和分组读取当前业务配置快照。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigTemplateImpl implements ConfigTemplate {
    private final ConfigCacheManager cache;
    private final ObjectMapper mapper;
    private final ConfigListenerTemplate listeners;
    private final String group;

    @Override
    public ConfigTemplate group(String group) {
        Assert.hasText(group, "Config group must not be blank");
        return new ConfigTemplateImpl(cache, mapper, listeners, group);
    }

    @Override
    public ConfigValueTemplate opsForValue() {
        return new ConfigValueTemplateImpl(key -> cache.find(key, group).map(ConfigItemDTO::getContent));
    }

    @Override
    public ConfigObjectTemplate opsForObject() {
        return new ConfigObjectTemplateImpl(key -> cache.find(key, group).map(ConfigItemDTO::getContent), mapper);
    }

    @Override
    public ConfigSubscriptionHandler subscribe(String key, ConfigChangeListener listener) {
        Assert.hasText(key, "Config key must not be blank");
        Assert.notNull(listener, "Config listener must not be null");
        return listeners.subscribe(key, group, listener);
    }
}
