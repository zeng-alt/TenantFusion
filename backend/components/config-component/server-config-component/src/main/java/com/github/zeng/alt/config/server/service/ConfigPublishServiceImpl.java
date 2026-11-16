package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.event.ConfigDataChangedEvent;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class ConfigPublishServiceImpl implements ConfigPublishService {
    private final ConfigStorageTemplate storage;
    private final ConfigSnapshotHelper snapshots;
    private final ApplicationEventPublisher publisher;

    @Override
    public Either<String, ConfigReleaseDto> publish(ConfigPublishCmd command) {
        return storage.execute(() -> storage.getApp(command.appCode()).toEither("应用不存在: " + command.appCode())
                .flatMap(app -> publish(app, command)))
                .peek(release -> notifyClients(command.appCode(), release));
    }

    private Either<String, ConfigReleaseDto> publish(ConfigAppDto app, ConfigPublishCmd command) {
        final List<Long> ids = command.configIds().stream().distinct().toList();
        final List<ConfigEntryDto> selected = storage.listConfigs(ids);
        if (ids.isEmpty() || selected.size() != ids.size()
                || selected.stream().anyMatch(config -> !Objects.equals(config.appId(), app.appId()))) {
            return Either.left("发布的配置不存在或不属于当前应用");
        }
        final Map<ConfigChangedKey, ConfigItemDTO> merged = new HashMap<>();
        current(app.appId()).forEach(item -> merged.put(new ConfigChangedKey(item.getDataId(), item.getGroup()), item));
        selected.forEach(config -> {
            final var key = new ConfigChangedKey(config.dataId(), config.group());
            if ("deleted".equals(config.status())) {
                merged.remove(key);
            } else {
                final ConfigEntryDto published = storage.updateConfig(config.toBuilder().status("published").build());
                merged.put(key, snapshots.toDto(published, app.appCode()));
            }
        });
        return Either.right(saveRelease(app, List.copyOf(merged.values()), command.releaseNote()));
    }

    @Override
    public Either<String, ConfigReleaseDto> rollback(ConfigRollbackCmd command) {
        return storage.execute(() -> storage.getApp(command.appCode()).toEither("应用不存在: " + command.appCode())
                .flatMap(app -> rollback(app))).peek(release -> notifyClients(command.appCode(), release));
    }

    private Either<String, ConfigReleaseDto> rollback(ConfigAppDto app) {
        final List<ConfigReleaseDto> previous = storage.listRecentReleases(app.appId(), 2);
        if (previous.size() < 2 || previous.get(1).snapshot() == null) {
            return Either.left("没有可回滚的上一份发布快照");
        }
        storage.updateRelease(previous.getFirst().toBuilder().status("rolled_back").build());
        return Either.right(saveRelease(
            app,
            snapshots.read(previous.get(1).snapshot()),
            "Rollback to " + previous.get(1).releaseVersion()
        ));
    }

    @Override
    public Either<String, List<ConfigItemDTO>> getPublishedConfigs(String appCode) {
        return storage.getApp(appCode).toEither("应用不存在: " + appCode).map(app -> current(app.appId()));
    }

    private List<ConfigItemDTO> current(Long appId) {
        return storage.getLatestRelease(appId).map(release -> snapshots.read(release.snapshot())).getOrElse(List.of());
    }

    private ConfigReleaseDto saveRelease(ConfigAppDto app, List<ConfigItemDTO> items, String note) {
        return storage.createRelease(ConfigReleaseDto.builder().appId(app.appId())
                .configIds(items.stream().map(item -> String.valueOf(item.getConfigId())).collect(Collectors.joining(",")))
                .snapshot(snapshots.write(items)).status("published").releaseNote(note)
                .releaseVersion(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))).build());
    }

    private void notifyClients(String appCode, ConfigReleaseDto release) {
        // execute 返回后自有事务已提交；若加入外层 Spring 事务，监听器仍等待外层提交。
        publisher.publishEvent(new ConfigDataChangedEvent(this, appCode, snapshots.read(release.snapshot())));
    }

    @Override
    public ConfigReleaseDto publish(Long appId, List<Long> configIds, String releaseNote) {
        return storage.getApp(appId).toEither("应用不存在: " + appId)
                .flatMap(app -> publish(new ConfigPublishCmd(app.appCode(), configIds, releaseNote)))
                .getOrElseThrow(ConfigException::new);
    }

    @Override
    public ConfigReleaseDto rollback(Long appId) {
        return storage.getApp(appId).toEither("应用不存在: " + appId)
                .flatMap(app -> rollback(new ConfigRollbackCmd(app.appCode()))).getOrElseThrow(ConfigException::new);
    }
}
