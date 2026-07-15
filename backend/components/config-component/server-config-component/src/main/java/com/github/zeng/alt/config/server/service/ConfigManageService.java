package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;

import java.util.List;

public interface ConfigManageService {

    ConfigInfoEntity createConfig(Long appId, String dataId, String group, String content, String format, String description);

    ConfigInfoEntity updateConfig(Long configId, String content, String description, String operator);

    ConfigInfoEntity getConfig(Long appId, String dataId, String group);

    List<ConfigItemDTO> getConfigs(Long appId, List<String> dataIds);

    List<ConfigItemDTO> getAllConfigs(Long appId);

    List<ConfigInfoEntity> listByAppId(Long appId);

    void deleteConfig(Long configId);

    ConfigItemDTO toDTO(ConfigInfoEntity entity);
}
