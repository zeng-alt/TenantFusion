package com.github.zeng.alt.storage.redisson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.github.zeng.alt.storage.*;
import com.github.zeng.alt.storage.redisson.suepr.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.JsonJackson3Codec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/**
 * Redisson 存储自动配置
 * 当 classpath 中存在 RedissonClient 且未自定义 StorageTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
@AutoConfiguration(before = {StorageAutoConfiguration.class})
@EnableConfigurationProperties({RedissonProperties.class})
@ConditionalOnClass(RedissonClient.class)
@ImportRuntimeHints(RedissonRuntimeHints.class)
public class RedissonStorageAutoConfiguration {

    private final RedissonProperties redissonProperties;


    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer(JsonJackson3Codec jsonCodec, KeyPrefixStrategy keyPrefixStrategy) {
        return config -> {
            // 组合序列化 key 使用 String 内容使用通用 json 格式
            CompositeCodec codec = new CompositeCodec(StringCodec.INSTANCE, jsonCodec, jsonCodec);

            config.setThreads(redissonProperties.getThreads())
                    .setNettyThreads(redissonProperties.getNettyThreads())
                    // 缓存 Lua 脚本 减少网络传输(redisson 大部分的功能都是基于 Lua 脚本实现)
                    .setUseScriptCache(true)
                    .setCodec(codec);

            RedissonProperties.SingleServerConfig singleServerConfig = redissonProperties.getSingleServerConfig();
            if (!Objects.isNull(singleServerConfig)) {
                // 使用单机模式
                config.useSingleServer()
                        //设置redis key前缀
                        .setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()).andThen(keyPrefixStrategy))
                        .setTimeout(singleServerConfig.getTimeout())
                        .setClientName(singleServerConfig.getClientName())
                        .setIdleConnectionTimeout(singleServerConfig.getIdleConnectionTimeout())
                        .setSubscriptionConnectionPoolSize(singleServerConfig.getSubscriptionConnectionPoolSize())
                        .setConnectionMinimumIdleSize(singleServerConfig.getConnectionMinimumIdleSize())
                        .setConnectionPoolSize(singleServerConfig.getConnectionPoolSize());
            }
            // 集群配置方式 参考下方注释
            RedissonProperties.ClusterServersConfig clusterServersConfig = redissonProperties.getClusterServersConfig();
            if (!Objects.isNull(clusterServersConfig)) {
                config.useClusterServers()
                        //设置redis key前缀
                        .setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()).andThen(keyPrefixStrategy))
                        .setTimeout(clusterServersConfig.getTimeout())
                        .setClientName(clusterServersConfig.getClientName())
                        .setIdleConnectionTimeout(clusterServersConfig.getIdleConnectionTimeout())
                        .setSubscriptionConnectionPoolSize(clusterServersConfig.getSubscriptionConnectionPoolSize())
                        .setMasterConnectionMinimumIdleSize(clusterServersConfig.getMasterConnectionMinimumIdleSize())
                        .setMasterConnectionPoolSize(clusterServersConfig.getMasterConnectionPoolSize())
                        .setSlaveConnectionMinimumIdleSize(clusterServersConfig.getSlaveConnectionMinimumIdleSize())
                        .setSlaveConnectionPoolSize(clusterServersConfig.getSlaveConnectionPoolSize())
                        .setReadMode(clusterServersConfig.getReadMode())
                        .setSubscriptionMode(clusterServersConfig.getSubscriptionMode());
            }
            log.info("初始化 redis 配置");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonJackson3Codec jsonJacksonCodec(
            ObjectProvider<JacksonModule> modules,
            ObjectProvider<JsonMapperBuilderCustomizer> customizers
    ) {
        final JsonMapper.Builder builder = JsonMapper.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        builder.addModules(modules.orderedStream().toList());
        NullValueBuilderCustomizer.INSTANCE.customize(builder);
        builder.addModule(new Jdk8DateModule())
                .changeDefaultVisibility(visibility -> visibility.withVisibility(
                        PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                .activateDefaultTypingAsProperty(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                        DefaultTyping.NON_FINAL, "@class")
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET);
        return new JsonJackson3Codec(builder.build());
    }



    @Bean
    public StorageTemplate redissonStorageTemplate(
            RedissonClient redissonClient,
            KeyPrefixStrategy keyPrefixStrategy,
            CacheStringOperations cacheStringOperations,
            CacheListOperations cacheListOperations,
            CacheHashOperations cacheHashOperations,
            CacheZSetOperations cacheZSetOperations
    ) {
        return new RedissonStorageTemplate(
                redissonClient, keyPrefixStrategy,
                cacheStringOperations,
                cacheListOperations,
                cacheHashOperations,
                cacheZSetOperations
        );
    }

    @Bean
    public CacheStringOperations redissonStringOperations(RedissonClient redissonClient) {
        return new RedissonStringOperations(redissonClient);
    }

    @Bean
    public CacheListOperations redissonListOperations(RedissonClient redissonClient) {
        return new RedissonListOperations(redissonClient);
    }

    @Bean
    public CacheHashOperations redissonHashOperations(RedissonClient redissonClient) {
        return new RedissonHashOperations(redissonClient);
    }

    @Bean
    public CacheZSetOperations redissonZSetOperations(RedissonClient redissonClient) {
        return new RedissonZSetOperations(redissonClient);
    }
}
