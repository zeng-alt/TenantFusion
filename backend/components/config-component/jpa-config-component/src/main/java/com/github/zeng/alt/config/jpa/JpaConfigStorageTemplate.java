package com.github.zeng.alt.config.jpa;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.server.repository.ConfigAppRepository;
import com.github.zeng.alt.config.server.repository.ConfigHistoryRepository;
import com.github.zeng.alt.config.server.repository.ConfigInfoRepository;
import com.github.zeng.alt.config.server.repository.ConfigReleaseRepository;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionOperations;
import com.github.zeng.alt.config.jpa.mapper.JpaConfigMapper;

/**
 * 配置服务的默认 JPA 存储实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class JpaConfigStorageTemplate implements ConfigStorageTemplate {
    private final ConfigAppRepository apps;
    private final ConfigInfoRepository configs;
    private final ConfigHistoryRepository histories;
    private final ConfigReleaseRepository releases;
    private final JpaConfigMapper mapper;
    private final TransactionOperations transactions;

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }

    @Override
    public ConfigAppDto createApp(ConfigAppDto app) {
        return mapper.toDto(apps.save(mapper.toEntity(app)));
    }

    @Override
    public Option<ConfigAppDto> getApp(Long appId) {
        return apps.findById(appId).map(mapper::toDto);
    }

    @Override
    public Option<ConfigAppDto> getApp(String appCode) {
        return apps.findByAppCode(appCode).map(mapper::toDto);
    }

    @Override
    public List<ConfigAppDto> listApps() {
        return apps.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public ConfigEntryDto createConfig(ConfigEntryDto config) {
        return mapper.toDto(configs.saveAndFlush(mapper.toEntity(config)));
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long configId) {
        return configs.findById(configId).map(mapper::toDto);
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long appId, String dataId, String group) {
        return configs.findByAppIdAndDataIdAndGroupName(appId, dataId, group).map(mapper::toDto);
    }

    @Override
    public List<ConfigEntryDto> listConfigs(Long appId) {
        return configs.findByAppId(appId).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<ConfigEntryDto> listConfigs(List<Long> configIds) {
        return configs.findByIdIn(configIds).stream().map(mapper::toDto).toList();
    }

    @Override
    public ConfigEntryDto updateConfig(ConfigEntryDto config) {
        return configs.findById(config.configId()).map(entity -> {
            mapper.merge(config, entity);
            return mapper.toDto(configs.saveAndFlush(entity));
        }).getOrElseThrow(() -> new ConfigException("配置不存在: " + config.configId()));
    }

    @Override
    public Option<ConfigEntryDto> deleteConfig(Long configId) {
        return configs.findById(configId).map(entity -> {
            entity.setStatus("deleted");
            return mapper.toDto(configs.saveAndFlush(entity));
        });
    }

    @Override
    public ConfigHistoryDto createHistory(ConfigHistoryDto history) {
        return mapper.toDto(histories.save(mapper.toEntity(history)));
    }

    @Override
    public List<ConfigHistoryDto> listHistories(Long configId) {
        return histories.findByConfigIdOrderByVersionDesc(configId).stream().map(mapper::toDto).toList();
    }

    @Override
    public ConfigReleaseDto createRelease(ConfigReleaseDto release) {
        return mapper.toDto(releases.save(mapper.toEntity(release)));
    }

    @Override
    public ConfigReleaseDto updateRelease(ConfigReleaseDto release) {
        return releases.findById(release.releaseId()).map(entity -> {
            mapper.merge(release, entity);
            return mapper.toDto(releases.save(entity));
        }).getOrElseThrow(() -> new ConfigException("发布记录不存在: " + release.releaseId()));
    }

    @Override
    public Option<ConfigReleaseDto> getLatestRelease(Long appId) {
        return releases.findTopByAppIdOrderByReleaseIdDesc(appId).map(mapper::toDto);
    }

    @Override
    public List<ConfigReleaseDto> listRecentReleases(Long appId, int limit) {
        return releases.findByAppIdOrderByReleaseIdDesc(appId, PageRequest.of(0, limit))
                .stream().map(mapper::toDto).toList();
    }
}
