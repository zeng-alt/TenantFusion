package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.event.ConfigDataChangedEvent;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;
import com.github.zeng.alt.config.server.listener.ConfigLongPollManager;
import com.github.zeng.alt.config.server.repository.ConfigAppRepository;
import com.github.zeng.alt.config.server.repository.ConfigInfoRepository;
import com.github.zeng.alt.config.server.repository.ConfigReleaseRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommonsLog
@Service
public class ConfigPublishServiceImpl implements ConfigPublishService {

    private final ConfigInfoRepository configInfoRepository;
    private final ConfigReleaseRepository configReleaseRepository;
    private final ConfigAppRepository configAppRepository;
    private final ConfigLongPollManager longPollManager;
    private final ApplicationEventPublisher eventPublisher;

    public ConfigPublishServiceImpl(ConfigInfoRepository configInfoRepository,
                                    ConfigReleaseRepository configReleaseRepository,
                                    ConfigAppRepository configAppRepository,
                                    ConfigLongPollManager longPollManager,
                                    ApplicationEventPublisher eventPublisher) {
        this.configInfoRepository = configInfoRepository;
        this.configReleaseRepository = configReleaseRepository;
        this.configAppRepository = configAppRepository;
        this.longPollManager = longPollManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ConfigReleaseEntity publish(Long appId, List<Long> configIds, String releaseNote) {
        List<ConfigInfoEntity> configs = configInfoRepository.findByIdIn(configIds)
                .stream()
                .filter(c -> Objects.equals(c.getAppId(), appId))
                .collect(Collectors.toList());

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("No valid configs found for appId: " + appId);
        }

        String idsStr = configs.stream()
                .map(c -> String.valueOf(c.getConfigId()))
                .collect(Collectors.joining(","));

        for (ConfigInfoEntity config : configs) {
            config.setStatus("published");
            configInfoRepository.save(config);
        }

        ConfigReleaseEntity release = new ConfigReleaseEntity();
        release.setAppId(appId);
        release.setConfigIds(idsStr);
        release.setReleaseVersion(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        release.setReleaseNote(releaseNote);
        release.setStatus("published");
        ConfigReleaseEntity saved = configReleaseRepository.save(release);

        List<ConfigItemDTO> items = configs.stream()
                .map(c -> {
                    ConfigItemDTO dto = new ConfigItemDTO();
                    dto.setConfigId(c.getConfigId());
                    dto.setDataId(c.getDataId());
                    dto.setGroup(c.getGroupName());
                    dto.setContent(c.getContent());
                    dto.setFormat(c.getFormat());
                    dto.setVersion(c.getVersion());
                    return dto;
                })
                .collect(Collectors.toList());

        String appCode = configAppRepository.findById(appId)
                .map(ConfigAppEntity::getAppCode)
                .getOrNull();

        List<String> changedKeys = configs.stream()
                .map(ConfigInfoEntity::getDataId)
                .collect(Collectors.toList());

        eventPublisher.publishEvent(new ConfigDataChangedEvent(this, appCode, items));

        longPollManager.notifyClients(appCode, changedKeys);

        log.info("Published config release: appId=" + appId + ", releaseVersion=" + saved.getReleaseVersion()
                + ", configs=" + idsStr);

        return saved;
    }

    @Override
    @Transactional
    public ConfigReleaseEntity rollback(Long appId) {
        ConfigReleaseEntity lastRelease = configReleaseRepository
                .findTopByAppIdOrderByReleaseIdDesc(appId)
                .getOrElseThrow(() -> new IllegalArgumentException("No previous release found for appId: " + appId));

        lastRelease.setStatus("rolled_back");
        configReleaseRepository.save(lastRelease);

        log.info("Rolled back release: appId=" + appId + ", releaseId=" + lastRelease.getReleaseId());
        return lastRelease;
    }
}
