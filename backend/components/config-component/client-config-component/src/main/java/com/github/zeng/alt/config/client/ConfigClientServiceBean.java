package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.model.ConfigItemDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigClientServiceBean implements ConfigClientService {

    private final ConfigCacheManager cacheManager;

    public ConfigClientServiceBean(ConfigCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public ConfigItemDTO getConfig(String appCode, String dataId, String group) {
        return cacheManager.get(dataId, group);
    }

    @Override
    public Map<String, ConfigItemDTO> getConfigs(String appCode, List<String> dataIds) {
        if (dataIds == null || dataIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dataIds.stream()
                .map(cacheManager::get)
                .filter(dto -> dto != null)
                .collect(Collectors.toMap(ConfigItemDTO::getDataId, dto -> dto));
    }

    @Override
    public Map<String, ConfigItemDTO> getAllConfigs(String appCode) {
        return cacheManager.getAll();
    }
}
