package com.github.zeng.alt.config.server;

import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 测试用自定义存储，验证 SPI 可独立于 JPA 实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class MemoryConfigStorageTemplate implements ConfigStorageTemplate {
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, ConfigAppDto> apps = new ConcurrentHashMap<>();
    private final Map<Long, ConfigEntryDto> configs = new ConcurrentHashMap<>();
    private final Map<Long, ConfigHistoryDto> histories = new ConcurrentHashMap<>();
    private final Map<Long, ConfigReleaseDto> releases = new ConcurrentHashMap<>();

    @Override
    public synchronized <T> T execute(Supplier<T> action) {
        final Map<Long, ConfigAppDto> previousApps = Map.copyOf(apps);
        final Map<Long, ConfigEntryDto> previousConfigs = Map.copyOf(configs);
        final Map<Long, ConfigHistoryDto> previousHistories = Map.copyOf(histories);
        final Map<Long, ConfigReleaseDto> previousReleases = Map.copyOf(releases);
        return Try.of(action::get).onFailure(error -> {
            apps.clear();
            apps.putAll(previousApps);
            configs.clear();
            configs.putAll(previousConfigs);
            histories.clear();
            histories.putAll(previousHistories);
            releases.clear();
            releases.putAll(previousReleases);
        }).get();
    }

    @Override
    public ConfigAppDto createApp(ConfigAppDto app) {
        final ConfigAppDto saved = app.toBuilder().appId(ids.incrementAndGet()).build();
        apps.put(saved.appId(), saved);
        return saved;
    }

    @Override
    public Option<ConfigAppDto> getApp(Long appId) {
        return Option.of(apps.get(appId));
    }

    @Override
    public Option<ConfigAppDto> getApp(String appCode) {
        return Option.ofOptional(apps.values().stream().filter(app -> app.appCode().equals(appCode)).findFirst());
    }

    @Override
    public List<ConfigAppDto> listApps() {
        return List.copyOf(apps.values());
    }

    @Override
    public ConfigEntryDto createConfig(ConfigEntryDto config) {
        final ConfigEntryDto saved = config.toBuilder().configId(ids.incrementAndGet()).version(0).build();
        configs.put(saved.configId(), saved);
        return saved;
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long configId) {
        return Option.of(configs.get(configId));
    }

    @Override
    public Option<ConfigEntryDto> getConfig(Long appId, String dataId, String group) {
        return Option.ofOptional(configs.values().stream().filter(config -> config.appId().equals(appId)
                && config.dataId().equals(dataId) && config.group().equals(group)).findFirst());
    }

    @Override
    public List<ConfigEntryDto> listConfigs(Long appId) {
        return configs.values().stream().filter(config -> config.appId().equals(appId)).toList();
    }

    @Override
    public List<ConfigEntryDto> listConfigs(List<Long> configIds) {
        return configIds.stream().flatMap(id -> getConfig(id).toJavaStream()).toList();
    }

    @Override
    public ConfigEntryDto updateConfig(ConfigEntryDto config) {
        final ConfigEntryDto saved = config.toBuilder().version(config.version() + 1).build();
        configs.put(saved.configId(), saved);
        return saved;
    }

    @Override
    public Option<ConfigEntryDto> deleteConfig(Long configId) {
        return getConfig(configId).map(config -> updateConfig(config.toBuilder().status("deleted").build()));
    }

    @Override
    public ConfigHistoryDto createHistory(ConfigHistoryDto history) {
        final ConfigHistoryDto saved = history.toBuilder().historyId(ids.incrementAndGet()).build();
        histories.put(saved.historyId(), saved);
        return saved;
    }

    @Override
    public List<ConfigHistoryDto> listHistories(Long configId) {
        return histories.values().stream().filter(history -> history.configId().equals(configId)).toList();
    }

    @Override
    public ConfigReleaseDto createRelease(ConfigReleaseDto release) {
        final ConfigReleaseDto saved = release.toBuilder().releaseId(ids.incrementAndGet()).build();
        releases.put(saved.releaseId(), saved);
        return saved;
    }

    @Override
    public ConfigReleaseDto updateRelease(ConfigReleaseDto release) {
        releases.put(release.releaseId(), release);
        return release;
    }

    @Override
    public Option<ConfigReleaseDto> getLatestRelease(Long appId) {
        return Option.ofOptional(listRecentReleases(appId, 1).stream().findFirst());
    }

    @Override
    public List<ConfigReleaseDto> listRecentReleases(Long appId, int limit) {
        return releases.values().stream().filter(release -> release.appId().equals(appId))
                .sorted(Comparator.comparing(ConfigReleaseDto::releaseId).reversed()).limit(limit).toList();
    }
}
