package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigClientServiceBean implements ConfigClientService {
    private final ConfigCacheManager cacheManager;

    @Override
    public ConfigItemDTO getConfig(String appCode, String dataId, String group) {
        requireApp(appCode);
        return cacheManager.get(dataId, group);
    }

    @Override
    public Map<String, ConfigItemDTO> getConfigs(String appCode, List<String> dataIds) {
        requireApp(appCode);
        if (dataIds == null || dataIds.isEmpty()) {
            return Map.of();
        }
        return dataIds.stream().distinct().flatMap(id -> cacheManager.find(id, null).toJavaStream())
                .collect(Collectors.toUnmodifiableMap(ConfigItemDTO::getDataId, item -> item));
    }

    @Override
    public Map<String, ConfigItemDTO> getAllConfigs(String appCode) {
        requireApp(appCode);
        return cacheManager.getAll();
    }

    private void requireApp(String appCode) {
        if (!cacheManager.getAppCode().equals(appCode)) {
            throw new ConfigException("客户端仅绑定应用: " + cacheManager.getAppCode());
        }
    }
}
