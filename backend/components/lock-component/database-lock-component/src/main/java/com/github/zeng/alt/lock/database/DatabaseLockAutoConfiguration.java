package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.LockTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

/**
 * 数据库分布式锁自动配置
 * 当 classpath 中存在 DataSource 且未自定义 LockTemplate 时自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(JdbcClient.class)
@ConditionalOnMissingBean(LockTemplate.class)
public class DatabaseLockAutoConfiguration {

    @Bean
    public LockTemplate databaseLockTemplate(DataSource dataSource) {
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        return new DatabaseLockTemplate(jdbcClient);
    }
}
