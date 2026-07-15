package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigDataChangedEvent;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class ConfigSubscriber {

    private final ConfigCacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ConfigClientProperties properties;

    public ConfigSubscriber(ConfigCacheManager cacheManager,
                            ApplicationEventPublisher eventPublisher,
                            ConfigClientProperties properties) {
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @EventListener
    public void onConfigDataChanged(ConfigDataChangedEvent event) {
        if (properties.getAppCode() != null && !properties.getAppCode().equals(event.getAppCode())
                && event.getAppCode() != null) {
            return;
        }

        List<ConfigItemDTO> items = event.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        cacheManager.updateAll(items);

        List<ConfigChangedKey> changedKeys = items.stream()
                .map(i -> new ConfigChangedKey(i.getDataId(), i.getGroup()))
                .collect(Collectors.toList());

        ConfigChangeEvent changeEvent = new ConfigChangeEvent(this, properties.getAppCode(), changedKeys);
        eventPublisher.publishEvent(changeEvent);

        log.info("Received config update for " + changedKeys.size() + " keys: " + changedKeys);
    }
}
