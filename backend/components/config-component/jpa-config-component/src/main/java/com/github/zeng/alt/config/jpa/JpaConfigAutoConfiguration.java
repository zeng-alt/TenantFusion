package com.github.zeng.alt.config.jpa;

import com.github.zeng.alt.config.jpa.aot.JpaConfigRuntimeHints;
import com.github.zeng.alt.config.jpa.mapper.JpaConfigMapper;
import com.github.zeng.alt.config.jpa.mapper.JpaConfigMapperImpl;
import com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration;
import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.repository.ConfigAppRepository;
import com.github.zeng.alt.config.server.repository.ConfigHistoryRepository;
import com.github.zeng.alt.config.server.repository.ConfigInfoRepository;
import com.github.zeng.alt.config.server.repository.ConfigReleaseRepository;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 未贡献自定义存储时，装配默认 JPA 适配。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(before = ServerConfigAutoConfiguration.class)
@ConditionalOnProperty(prefix = "config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(ConfigStorageTemplate.class)
@AutoConfigurationPackage(basePackageClasses = {ConfigAppEntity.class, ConfigAppRepository.class})
@ImportRuntimeHints(JpaConfigRuntimeHints.class)
public class JpaConfigAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JpaConfigMapper jpaConfigMapper() {
        return new JpaConfigMapperImpl();
    }

    @Configuration(proxyBeanMethods = false)
    @RequiredArgsConstructor
    static class StorageConfiguration {
        private final PlatformTransactionManager transactions;

        @Bean
        @ConditionalOnMissingBean(ConfigStorageTemplate.class)
        ConfigStorageTemplate configStorageTemplate(
            ConfigAppRepository apps,
            ConfigInfoRepository configs,
            ConfigHistoryRepository histories,
            ConfigReleaseRepository releases,
            JpaConfigMapper mapper
        ) {
            return new JpaConfigStorageTemplate(
                apps,
                configs,
                histories,
                releases,
                mapper,
                new TransactionTemplate(transactions)
            );
        }
    }
}
