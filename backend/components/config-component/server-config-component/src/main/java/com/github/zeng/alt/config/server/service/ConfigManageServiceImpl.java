package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.mapper.ConfigMapper;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigUpdateCmd;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigManageServiceImpl implements ConfigManageService {
    private final ConfigStorageTemplate storage;
    private final ConfigMapper mapper;

    @Override
    public Either<String, ConfigAppDto> createApp(ConfigAppCmd command) {
        return storage.execute(() -> {
            if (storage.getApp(command.appCode()).isDefined()) {
                return Either.left("应用已存在: " + command.appCode());
            }
            return Either.right(storage.createApp(mapper.toApp(command)));
        });
    }

    @Override
    public Either<String, ConfigAppDto> getApp(String appCode) {
        return storage.getApp(appCode).toEither("应用不存在: " + appCode);
    }

    @Override
    public List<ConfigAppDto> listApps() {
        return storage.listApps();
    }

    @Override
    public Either<String, ConfigEntryDto> create(ConfigCreateCmd command) {
        return storage.execute(() -> getApp(command.appCode()).flatMap(app -> {
            final String group = Option.of(command.group()).getOrElse(ConfigTemplate.DEFAULT_GROUP);
            if (storage.getConfig(app.appId(), command.dataId(), group).isDefined()) {
                return Either.left("配置已存在: " + command.dataId());
            }
            final ConfigEntryDto saved = storage.createConfig(mapper.toEntry(command).toBuilder().appId(app.appId()).build());
            saveHistory(saved, "CREATE", null);
            return Either.right(saved);
        }));
    }

    @Override
    public Either<String, ConfigEntryDto> update(Long configId, ConfigUpdateCmd command) {
        return storage.execute(() -> storage.getConfig(configId).toEither("配置不存在: " + configId).map(config -> {
            final ConfigEntryDto saved = storage.updateConfig(config.toBuilder()
                    .content(command.content()).description(Option.of(command.description()).getOrElse(config.description()))
                    .status("draft").build());
            saveHistory(saved, "UPDATE", command.operator());
            return saved;
        }));
    }

    @Override
    public Either<String, Long> delete(Long configId) {
        return storage.execute(() -> storage.deleteConfig(configId).toEither("配置不存在: " + configId)
                .peek(config -> saveHistory(config, "DELETE", null)).map(ConfigEntryDto::configId));
    }

    @Override
    public ConfigEntryDto createConfig(
        Long appId,
        String dataId,
        String group,
        String content,
        String format,
        String description
    ) {
        return storage.getApp(appId).toEither("应用不存在: " + appId)
                .flatMap(app -> create(new ConfigCreateCmd(app.appCode(), dataId, group, content, format, description)))
                .getOrElseThrow(ConfigException::new);
    }

    @Override
    public ConfigEntryDto updateConfig(Long configId, String content, String description, String operator) {
        return update(configId, new ConfigUpdateCmd(content, description, operator)).getOrElseThrow(ConfigException::new);
    }

    @Override
    public ConfigEntryDto getConfig(Long appId, String dataId, String group) {
        return storage.getConfig(appId, dataId, Option.of(group).getOrElse(ConfigTemplate.DEFAULT_GROUP)).getOrNull();
    }

    @Override
    public List<ConfigItemDTO> getConfigs(Long appId, List<String> dataIds) {
        return storage.listConfigs(appId).stream().filter(config -> dataIds.contains(config.dataId())).map(mapper::toDto).toList();
    }

    @Override
    public List<ConfigItemDTO> getAllConfigs(Long appId) {
        return storage.listConfigs(appId).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<ConfigEntryDto> listByAppId(Long appId) {
        return storage.listConfigs(appId);
    }

    @Override
    public void deleteConfig(Long configId) {
        delete(configId).getOrElseThrow(ConfigException::new);
    }

    @Override
    public ConfigItemDTO toDTO(ConfigEntryDto config) {
        return mapper.toDto(config);
    }

    private void saveHistory(ConfigEntryDto config, String operation, String operator) {
        storage.createHistory(mapper.toHistory(config).toBuilder().operation(operation).operator(operator)
                .createdDate(LocalDateTime.now()).build());
    }
}
