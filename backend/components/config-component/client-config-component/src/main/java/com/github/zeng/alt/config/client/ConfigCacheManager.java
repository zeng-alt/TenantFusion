package com.github.zeng.alt.config.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class ConfigCacheManager {
    private final Path cacheFile;
    @Getter
    private final String appCode;
    private final ObjectMapper mapper;
    private volatile Map<ConfigChangedKey, ConfigItemDTO> snapshot = Map.of();
    private volatile boolean initialized;

    public ConfigCacheManager(String cacheDir, String appCode) {
        this(cacheDir, appCode, new JsonMapper());
    }

    public ConfigCacheManager(String cacheDir, String appCode, ObjectMapper mapper) {
        this.mapper = mapper;
        this.appCode = appCode;
        final Path directory = StringUtils.hasText(cacheDir)
                ? Path.of(cacheDir) : Path.of(System.getProperty("user.home", "."), ".config-cache");
        this.cacheFile = directory.resolve(appCode + "-config-cache.json");
        loadFromDisk();
    }

    /** 合并增量配置；远程完整快照使用 replaceAll。 */
    public synchronized void updateAll(List<ConfigItemDTO> items) {
        final Map<ConfigChangedKey, ConfigItemDTO> merged = new HashMap<>(snapshot);
        items.forEach(item -> merged.put(key(item), copy(item)));
        store(merged);
    }

    /** 替换完整快照，包含删除和空快照。 */
    public synchronized void replaceAll(List<ConfigItemDTO> items) {
        store(items.stream().collect(Collectors.toMap(this::key, this::copy)));
    }

    public synchronized void update(String dataId, ConfigItemDTO item) {
        final Map<ConfigChangedKey, ConfigItemDTO> updated = new HashMap<>(snapshot);
        if (item == null) {
            updated.remove(new ConfigChangedKey(dataId, ConfigTemplate.DEFAULT_GROUP));
        } else {
            updated.put(new ConfigChangedKey(dataId, key(item).getGroup()), copy(item));
        }
        store(updated);
    }

    /** 兼容旧接口；缺失时返回 null。新代码使用 ConfigTemplate。 */
    public ConfigItemDTO get(String dataId) {
        return get(dataId, ConfigTemplate.DEFAULT_GROUP);
    }

    public ConfigItemDTO get(String dataId, String group) {
        return find(dataId, group).getOrNull();
    }

    public Option<ConfigItemDTO> find(String dataId, String group) {
        return Option.of(snapshot.get(new ConfigChangedKey(
            dataId,
            Option.of(group).getOrElse(ConfigTemplate.DEFAULT_GROUP)
        ))).map(this::copy);
    }

    /** 旧批量接口限定默认分组，避免以 dataId 为键时丢失同名配置。 */
    public Map<String, ConfigItemDTO> getAll() {
        return snapshot.values().stream()
                .filter(item -> ConfigTemplate.DEFAULT_GROUP.equals(item.getGroup()))
                .collect(Collectors.toUnmodifiableMap(ConfigItemDTO::getDataId, this::copy));
    }

    public boolean isInitialized() {
        return initialized;
    }

    Map<ConfigChangedKey, ConfigItemDTO> getSnapshot() {
        return snapshot;
    }

    private ConfigChangedKey key(ConfigItemDTO item) {
        return new ConfigChangedKey(
            item.getDataId(),
            Option.of(item.getGroup()).getOrElse(ConfigTemplate.DEFAULT_GROUP)
        );
    }

    private ConfigItemDTO copy(ConfigItemDTO item) {
        final var copy = new ConfigItemDTO(item.getDataId(), key(item).getGroup(), item.getContent(), item.getFormat());
        copy.setConfigId(item.getConfigId());
        copy.setVersion(item.getVersion());
        copy.setAppCode(item.getAppCode());
        return copy;
    }

    private void store(Map<ConfigChangedKey, ConfigItemDTO> items) {
        snapshot = Map.copyOf(items);
        initialized = true;
        Try.run(() -> {
            Files.createDirectories(cacheFile.getParent());
            mapper.writeValue(cacheFile.toFile(), snapshot.values());
        }).onFailure(error -> log.warn(
            "Unable to save config cache at {} ({})",
            cacheFile,
            error.getClass().getSimpleName()
        ));
    }

    private void loadFromDisk() {
        if (!Files.exists(cacheFile)) {
            return;
        }
        Try.of(() -> mapper.readValue(cacheFile.toFile(), new TypeReference<List<ConfigItemDTO>>() {}))
                .map(items -> items.stream().collect(Collectors.toUnmodifiableMap(this::key, this::copy)))
                .onSuccess(items -> {
                    snapshot = items;
                    initialized = true;
                })
                .onFailure(error -> log.warn(
                    "Unable to load config cache at {} ({})",
                    cacheFile,
                    error.getClass().getSimpleName()
                ));
    }
}
