package com.github.zeng.alt.storage.redisson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.github.zeng.alt.storage.*;
import com.github.zeng.alt.storage.redisson.suepr.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
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
@AutoConfiguration(before = StorageAutoConfiguration.class)
@EnableConfigurationProperties({RedissonProperties.class})
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnMissingBean(StorageTemplate.class)
public class RedissonStorageAutoConfiguration {

    private final RedissonProperties redissonProperties;


    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer(JsonJacksonCodec jsonCodec, KeyPrefixStrategy keyPrefixStrategy) {
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
    public JsonJacksonCodec jsonJacksonCodec(ObjectProvider<Module> modules, List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
        NullValueBuilderCustomizer.INSTANCE.customize(builder);
        ObjectMapper objectMapper = builder.build();
        objectMapper.registerModules(modules);
        objectMapper.registerModule(new Jdk8DateModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//			// 指定序列化输入的类型，类必须是非final修饰的。序列化时将对象全类名一起保存下来
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        return new JsonJacksonCodec(objectMapper);
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
    public CacheStringOperations noOpCacheStringOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonStringOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheListOperations noOpCacheListOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonListOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheHashOperations noOpCacheHashOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonHashOperations(redissonClient, keyPrefixStrategy);
    }

    @Bean
    public CacheZSetOperations noOpCacheZSetOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        return new RedissonZSetOperations(redissonClient, keyPrefixStrategy);
    }
}
