package com.github.zeng.alt.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigSourceTemplate;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.client.aot.ConfigBindingAotProcessor;
import com.github.zeng.alt.config.client.aot.ConfigClientRuntimeHints;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AutoConfiguration(afterName = "com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration")
@ConditionalOnProperty(prefix = "config.client", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "config.client", name = "app-code")
@EnableConfigurationProperties(ConfigClientProperties.class)
@ImportRuntimeHints(ConfigClientRuntimeHints.class)
public class ConfigClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ConfigCacheManager configCacheManager(
        ConfigClientProperties properties,
        ObjectProvider<ObjectMapper> mappers,
        ObjectProvider<ConfigSourceTemplate> sources,
        ConfigRemoteTemplate remote
    ) {
        final var cache = new ConfigCacheManager(properties.getCacheDir(), properties.getAppCode(), mapper(mappers));
        if (properties.isRefreshEnabled()) {
            Try.of(() -> Option.of(sources.getIfAvailable())
                    .map(source -> source.getAll(properties.getAppCode()).getOrElseThrow(ConfigException::new))
                    .getOrElse(remote::getAll))
                    .onSuccess(cache::replaceAll)
                    .onFailure(error -> log.warn(
                        "Initial config fetch failed for app={} ({})",
                        properties.getAppCode(),
                        error.getClass().getSimpleName()
                    ));
        }
        return cache;
    }

    @Bean
    public static ConfigBindingAotProcessor configBindingAotProcessor() {
        return new ConfigBindingAotProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public static ConfigRefreshScope configRefreshScope(ObjectProvider<ConfigClientProperties> properties) {
        return new ConfigRefreshScope(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public static ConfigAnnotationBeanPostProcessor configAnnotationBeanPostProcessor(
            ConfigurableListableBeanFactory beans
    ) {
        return new ConfigAnnotationBeanPostProcessor(beans);
    }

    @Bean
    @ConditionalOnMissingBean
    public static ConfigPropertiesBeanPostProcessor configPropertiesBeanPostProcessor(
        ConfigurableListableBeanFactory beans
    ) {
        return new ConfigPropertiesBeanPostProcessor(beans);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigPropertiesBindingTemplate configPropertiesBindingTemplate(
        ConfigCacheManager cache,
        ConfigurableEnvironment environment,
        ObjectProvider<ObjectMapper> mappers
    ) {
        return new ConfigPropertiesBindingTemplate(cache, environment, mapper(mappers));
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigSubscriber configSubscriber(
        ConfigCacheManager cache,
        ApplicationEventPublisher publisher,
        ConfigClientProperties properties
    ) {
        return new ConfigSubscriber(cache, publisher, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigListenerTemplate configListenerTemplate(
        ConfigClientProperties properties,
        ObjectProvider<ConfigChangeListener> listeners
    ) {
        return new ConfigListenerTemplate(properties, listeners);
    }

    @Bean
    @ConditionalOnMissingBean(ConfigTemplate.class)
    public ConfigTemplate configTemplate(
        ConfigCacheManager cache,
        ObjectProvider<ObjectMapper> mappers,
        ConfigListenerTemplate listeners,
        ConfigClientProperties properties
    ) {
        return new ConfigTemplateImpl(cache, mapper(mappers), listeners, properties.getDefaultGroup());
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigRemoteTemplate configRemoteTemplate(
        ConfigClientProperties properties,
        ObjectProvider<ObjectMapper> mappers,
        ObjectProvider<ConfigHttpClientCustomizer> customizers
    ) {
        return new ConfigRemoteTemplate(
            properties,
            mapper(mappers),
            client(properties, properties.getRequestTimeoutMs(), customizers),
            client(properties, (int) properties.getLongPollTimeoutMs() + properties.getRequestTimeoutMs(), customizers)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigStartupInitializer configStartupInitializer(
        ConfigRemoteTemplate remote,
        ConfigSubscriber subscriber,
        ConfigClientProperties properties,
        ObjectProvider<ConfigSourceTemplate> sources
    ) {
        return new ConfigStartupInitializer(remote, subscriber, properties, sources);
    }

    @Bean
    @ConditionalOnMissingBean(ConfigClientService.class)
    public ConfigClientService configClientServiceBean(ConfigCacheManager cache) {
        return new ConfigClientServiceBean(cache);
    }

    private ObjectMapper mapper(ObjectProvider<ObjectMapper> mappers) {
        return mappers.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build());
    }

    private RestTemplate client(
        ConfigClientProperties properties,
        int readTimeout,
        ObjectProvider<ConfigHttpClientCustomizer> customizers
    ) {
        final var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(readTimeout);
        final var client = new RestTemplate(factory);
        customizers.orderedStream().forEach(customizer -> customizer.customize(client));
        return client;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.context.refresh.ContextRefresher")
    @ConditionalOnBean(type = "org.springframework.cloud.context.refresh.ContextRefresher")
    @ConditionalOnProperty(prefix = "config.client", name = "refresh-scope-enabled", havingValue = "true")
    static class RefreshConfiguration {
        @Bean
        @ConditionalOnMissingBean
        ConfigRefreshBridge configRefreshBridge(ContextRefresher refresher) {
            return new ConfigRefreshBridge(refresher);
        }
    }
}
