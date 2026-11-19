package com.github.zeng.alt.config.server.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigSourceTemplate;
import com.github.zeng.alt.config.server.aot.ConfigServerRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.server.controller.ConfigClientController;
import com.github.zeng.alt.config.server.controller.ConfigLongPollController;
import com.github.zeng.alt.config.server.controller.ConfigManageController;
import com.github.zeng.alt.config.server.listener.ConfigLongPollManager;
import com.github.zeng.alt.config.server.mapper.ConfigMapper;
import com.github.zeng.alt.config.server.mapper.ConfigMapperImpl;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigManageServiceImpl;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import com.github.zeng.alt.config.server.service.ConfigPublishServiceImpl;
import com.github.zeng.alt.config.server.service.ConfigSnapshotHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ImportRuntimeHints(ConfigServerRuntimeHints.class)
@ConditionalOnProperty(prefix = "config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServerConfigAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ConfigMapper.class)
    public ConfigMapper configMapper() {
        return new ConfigMapperImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigSnapshotHelper configSnapshotHelper(ObjectProvider<ObjectMapper> mappers, ConfigMapper mapper) {
        return new ConfigSnapshotHelper(
            mappers.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build()),
            mapper
        );
    }

    @Bean
    @ConditionalOnMissingBean(ConfigManageService.class)
    public ConfigManageService configManageService(ConfigStorageTemplate storage, ConfigMapper mapper) {
        return new ConfigManageServiceImpl(storage, mapper);
    }

    @Bean
    @ConditionalOnMissingBean(ConfigPublishService.class)
    public ConfigPublishService configPublishService(
        ConfigStorageTemplate storage,
        ConfigSnapshotHelper snapshots,
        ApplicationEventPublisher publisher
    ) {
        return new ConfigPublishServiceImpl(storage, snapshots, publisher);
    }

    @Bean
    @ConditionalOnMissingBean(ConfigSourceTemplate.class)
    public ConfigSourceTemplate configSourceTemplate(ConfigPublishService publish) {
        return publish::getPublishedConfigs;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigLongPollManager configLongPollManager() {
        return new ConfigLongPollManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigClientController configClientController(ConfigPublishService publish) {
        return new ConfigClientController(publish);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigManageController configManageController(ConfigManageService manage, ConfigPublishService publish) {
        return new ConfigManageController(manage, publish);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigLongPollController configLongPollController(ConfigLongPollManager manager) {
        return new ConfigLongPollController(manager);
    }
}
