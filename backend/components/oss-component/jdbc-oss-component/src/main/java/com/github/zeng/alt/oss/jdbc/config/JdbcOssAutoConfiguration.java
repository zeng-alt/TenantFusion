package com.github.zeng.alt.oss.jdbc.config;

import com.github.zeng.alt.oss.jdbc.dao.OssFileDao;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * OSS JDBC 模块自动配置。
 * <p>
 * 提供基于 JDBC 的文件记录持久化能力，作为 JPA 版本的轻量级替代。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnBean(DataSource.class)
public class JdbcOssAutoConfiguration {

    /**
     * 创建 JDBC 文件记录 DAO。
     */
    @Bean
    @ConditionalOnMissingBean
    public OssFileDao ossFileDao(NamedParameterJdbcTemplate jdbcTemplate) {
        return new OssFileDao(jdbcTemplate);
    }
}
