package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.model.ConfigItemDTO;

import java.util.List;
import java.util.Map;

public interface ConfigClientService {

    ConfigItemDTO getConfig(String appCode, String dataId, String group);

    Map<String, ConfigItemDTO> getConfigs(String appCode, List<String> dataIds);

    Map<String, ConfigItemDTO> getAllConfigs(String appCode);
}
