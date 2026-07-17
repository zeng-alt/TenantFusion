package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.client.aot.ConfigClientRuntimeHints;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.web.client.RestTemplate;

@CommonsLog
@AutoConfiguration
@AutoConfigureAfter(name = "com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration")
@ConditionalOnProperty(prefix = "config.client", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ConfigClientProperties.class)
@ImportRuntimeHints(ConfigClientRuntimeHints.class)
public class ConfigClientAutoConfiguration {

//    @Bean
//    @ConditionalOnMissingBean
//    public RestTemplate configClientRestTemplate() {
//        return new RestTemplate();
//    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "config.client", name = "app-code")
    public ConfigCacheManager configCacheManager(ConfigClientProperties properties) {
        log.info("Creating ConfigCacheManager for appCode=" + properties.getAppCode());
        return new ConfigCacheManager(properties.getCacheDir(), properties.getAppCode());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "config.client", name = "app-code")
    public ConfigSubscriber configSubscriber(ConfigCacheManager cacheManager,
                                              ApplicationEventPublisher eventPublisher,
                                              ConfigClientProperties properties) {
        return new ConfigSubscriber(cacheManager, eventPublisher, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.cloud.context.refresh.ContextRefresher")
    public ConfigRefreshBridge configRefreshBridge(ContextRefresher contextRefresher) {
        return new ConfigRefreshBridge(contextRefresher);
    }

    @Bean
    @ConditionalOnBean(RestTemplate.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "config.client", name = "app-code")
    public ConfigStartupInitializer configStartupInitializer(ConfigCacheManager cacheManager,
                                                              ConfigClientProperties properties,
                                                              RestTemplate restTemplate) {
        return new ConfigStartupInitializer(cacheManager, properties, restTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "config.client", name = "app-code")
    public ConfigClientServiceBean configClientServiceBean(ConfigCacheManager cacheManager) {
        return new ConfigClientServiceBean(cacheManager);
    }
}
