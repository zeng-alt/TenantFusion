package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

/**
 * 数据库分布式锁自动配置
 * 提供基于 sys_distributed_lock 表的分布式锁模板和执行器
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(JdbcClient.class)
public class DatabaseLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockExecutor<?> databaseLockExecutor(DataSource dataSource) {
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        return new DatabaseLockExecutor(jdbcClient);
    }

    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate databaseLockTemplate(DataSource dataSource) {
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        return new DatabaseLockTemplate(jdbcClient);
    }
}
