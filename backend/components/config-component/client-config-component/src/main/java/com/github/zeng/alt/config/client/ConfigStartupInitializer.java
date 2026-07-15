package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@CommonsLog
public class ConfigStartupInitializer {

    private final ConfigCacheManager cacheManager;
    private final ConfigClientProperties properties;
    private final RestTemplate restTemplate;

    public ConfigStartupInitializer(ConfigCacheManager cacheManager,
                                    ConfigClientProperties properties,
                                    RestTemplate restTemplate) {
        this.cacheManager = cacheManager;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Initializing config client for appCode=" + properties.getAppCode());

        if (cacheManager.isInitialized()) {
            log.info("Config cache already initialized from local disk, skipping initial fetch");
            return;
        }

        try {
            String url = properties.getServerAddr() + "/api/config/fetch?appCode=" + properties.getAppCode();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                if (dataList != null) {
                    List<ConfigItemDTO> items = dataList.stream().map(this::mapToDTO).toList();
                    cacheManager.updateAll(items);
                    log.info("Initial config fetch complete: " + items.size() + " configs loaded");
                }
            }
        } catch (Exception e) {
            log.warn("Initial config fetch failed (server may not be ready yet), using local cache: " + e.getMessage());
            if (!cacheManager.isInitialized()) {
                log.warn("No local cache available, config values will be empty until first successful fetch");
            }
        }
    }

    private ConfigItemDTO mapToDTO(Map<String, Object> map) {
        ConfigItemDTO dto = new ConfigItemDTO();
        Object configId = map.get("configId");
        if (configId instanceof Number num) dto.setConfigId(num.longValue());
        dto.setDataId((String) map.get("dataId"));
        dto.setGroup((String) map.get("group"));
        dto.setContent((String) map.get("content"));
        dto.setFormat((String) map.get("format"));
        Object version = map.get("version");
        if (version instanceof Number num) dto.setVersion(num.intValue());
        dto.setAppCode(properties.getAppCode());
        return dto;
    }
}
