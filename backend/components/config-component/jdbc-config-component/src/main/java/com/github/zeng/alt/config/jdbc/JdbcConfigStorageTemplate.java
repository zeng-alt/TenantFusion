package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionOperations;

/**
 * 基于 Spring JDBC 的服务端存储实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class JdbcConfigStorageTemplate implements ConfigStorageTemplate {
    private final JdbcConfigAppRepository apps;
    private final JdbcConfigEntryRepository configs;
    private final JdbcConfigHistoryRepository histories;
    private final JdbcConfigReleaseRepository releases;
    private final TransactionOperations transactions;

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }

    @Override
    public ConfigAppDto createApp(ConfigAppDto app) {
        return apps.create(app);
    }

    @Override
    public Option<ConfigAppDto> getApp(Long appId) {
        return apps.get(appId);
    }

    @Override
    public Option<ConfigAppDto> getApp(String appCode) {
        return apps.get(appCode);
    }

    @Override
    public List<ConfigAppDto> listApps() {
        return apps.list();
    }

    @Override
    public ConfigEntryDto createConfig(ConfigEntryDto config) {
        return configs.create(config);
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long configId) {
        return configs.get(configId);
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long appId, String dataId, String group) {
        return configs.get(appId, dataId, group);
    }

    @Override
    public List<ConfigEntryDto> listConfigs(Long appId) {
        return configs.list(appId);
    }

    @Override
    public List<ConfigEntryDto> listConfigs(List<Long> configIds) {
        return configs.list(configIds);
    }

    @Override
    public ConfigEntryDto updateConfig(ConfigEntryDto config) {
        return configs.update(config);
    }

    @Override
    public Option<ConfigEntryDto> deleteConfig(Long configId) {
        return configs.delete(configId);
    }

    @Override
    public ConfigHistoryDto createHistory(ConfigHistoryDto history) {
        return histories.create(history);
    }

    @Override
    public List<ConfigHistoryDto> listHistories(Long configId) {
        return histories.list(configId);
    }

    @Override
    public ConfigReleaseDto createRelease(ConfigReleaseDto release) {
        return releases.create(release);
    }

    @Override
    public ConfigReleaseDto updateRelease(ConfigReleaseDto release) {
        return releases.update(release);
    }

    @Override
    public Option<ConfigReleaseDto> getLatestRelease(Long appId) {
        return releases.getLatest(appId);
    }

    @Override
    public List<ConfigReleaseDto> listRecentReleases(Long appId, int limit) {
        return releases.listRecent(appId, limit);
    }
}
