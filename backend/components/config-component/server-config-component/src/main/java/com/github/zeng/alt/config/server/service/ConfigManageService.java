package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigUpdateCmd;
import io.vavr.control.Either;
import java.util.List;

public interface ConfigManageService {
    Either<String, ConfigAppDto> createApp(ConfigAppCmd command);

    Either<String, ConfigAppDto> getApp(String appCode);

    List<ConfigAppDto> listApps();

    Either<String, ConfigEntryDto> create(ConfigCreateCmd command);

    Either<String, ConfigEntryDto> update(Long configId, ConfigUpdateCmd command);

    Either<String, Long> delete(Long configId);

    /** 兼容旧参数签名；新代码使用 create(ConfigCreateCmd)。 */
    ConfigEntryDto createConfig(
        Long appId,
        String dataId,
        String group,
        String content,
        String format,
        String description
    );

    ConfigEntryDto updateConfig(Long configId, String content, String description, String operator);

    ConfigEntryDto getConfig(Long appId, String dataId, String group);

    List<ConfigItemDTO> getConfigs(Long appId, List<String> dataIds);

    List<ConfigItemDTO> getAllConfigs(Long appId);

    List<ConfigEntryDto> listByAppId(Long appId);

    void deleteConfig(Long configId);

    ConfigItemDTO toDTO(ConfigEntryDto entity);
}
