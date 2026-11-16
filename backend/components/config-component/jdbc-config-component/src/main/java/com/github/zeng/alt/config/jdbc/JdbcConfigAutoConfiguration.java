package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC 配置存储自动装配，自定义存储和已装配的 JPA 存储优先。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(
    before = ServerConfigAutoConfiguration.class,
    after = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class},
    afterName = "com.github.zeng.alt.config.jpa.JpaConfigAutoConfiguration"
)
@ConditionalOnProperty(prefix = "config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(ConfigStorageTemplate.class)
public class JdbcConfigAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ConfigStorageTemplate.class)
    public ConfigStorageTemplate configStorageTemplate(DataSource source, PlatformTransactionManager transactions) {
        final var jdbc = new JdbcConfigRepositoryHelper(JdbcClient.create(source));
        return new JdbcConfigStorageTemplate(
            new JdbcConfigAppRepository(jdbc),
            new JdbcConfigEntryRepository(jdbc),
            new JdbcConfigHistoryRepository(jdbc),
            new JdbcConfigReleaseRepository(jdbc),
            new TransactionTemplate(transactions)
        );
    }
}
