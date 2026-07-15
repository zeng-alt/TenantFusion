package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigHistoryEntity;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.config.server.repository.ConfigHistoryRepository;
import com.github.zeng.alt.config.server.repository.ConfigInfoRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
@Service
public class ConfigManageServiceImpl implements ConfigManageService {

    private final ConfigInfoRepository configInfoRepository;
    private final ConfigHistoryRepository configHistoryRepository;

    public ConfigManageServiceImpl(ConfigInfoRepository configInfoRepository,
                                   ConfigHistoryRepository configHistoryRepository) {
        this.configInfoRepository = configInfoRepository;
        this.configHistoryRepository = configHistoryRepository;
    }

    @Override
    @Transactional
    public ConfigInfoEntity createConfig(Long appId, String dataId, String group, String content, String format, String description) {
        ConfigInfoEntity entity = new ConfigInfoEntity();
        entity.setAppId(appId);
        entity.setDataId(dataId);
        entity.setGroupName(group != null ? group : "DEFAULT_GROUP");
        entity.setContent(content);
        entity.setFormat(format != null ? format : "properties");
        entity.setDescription(description);
        entity.setStatus("draft");
        entity.setVersion(0);
        ConfigInfoEntity saved = configInfoRepository.save(entity);
        saveHistory(saved, "CREATE", null);
        log.info("Created config: appId=" + appId + ", dataId=" + dataId + ", group=" + group);
        return saved;
    }

    @Override
    @Transactional
    public ConfigInfoEntity updateConfig(Long configId, String content, String description, String operator) {
        ConfigInfoEntity entity = configInfoRepository.findById(configId)
                .getOrElseThrow(() -> new IllegalArgumentException("Config not found: " + configId));
        String oldContent = entity.getContent();
        entity.setContent(content);
        if (description != null) {
            entity.setDescription(description);
        }
        ConfigInfoEntity saved = configInfoRepository.save(entity);
        saveHistory(saved, "UPDATE", operator);
        log.info("Updated config: configId=" + configId + ", version=" + saved.getVersion());
        return saved;
    }

    @Override
    public ConfigInfoEntity getConfig(Long appId, String dataId, String group) {
        String g = group != null ? group : "DEFAULT_GROUP";
        return configInfoRepository.findByAppIdAndDataIdAndGroupName(appId, dataId, g)
                .getOrNull();
    }

    @Override
    public List<ConfigItemDTO> getConfigs(Long appId, List<String> dataIds) {
        return configInfoRepository.findByAppIdAndDataIdIn(appId, dataIds)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConfigItemDTO> getAllConfigs(Long appId) {
        return configInfoRepository.findByAppId(appId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConfigInfoEntity> listByAppId(Long appId) {
        return configInfoRepository.findByAppId(appId);
    }

    @Override
    @Transactional
    public void deleteConfig(Long configId) {
        configInfoRepository.findById(configId).peek(entity -> {
            saveHistory(entity, "DELETE", null);
            configInfoRepository.delete(entity);
        });
    }

    @Override
    public ConfigItemDTO toDTO(ConfigInfoEntity entity) {
        if (entity == null) return null;
        ConfigItemDTO dto = new ConfigItemDTO();
        dto.setConfigId(entity.getConfigId());
        dto.setDataId(entity.getDataId());
        dto.setGroup(entity.getGroupName());
        dto.setContent(entity.getContent());
        dto.setFormat(entity.getFormat());
        dto.setVersion(entity.getVersion());
        return dto;
    }

    private void saveHistory(ConfigInfoEntity entity, String operation, String operator) {
        ConfigHistoryEntity history = new ConfigHistoryEntity();
        history.setConfigId(entity.getConfigId());
        history.setContent(entity.getContent());
        history.setVersion(entity.getVersion());
        history.setOperation(operation);
        history.setOperator(operator);
        history.setCreatedDate(LocalDateTime.now());
        configHistoryRepository.save(history);
    }
}
