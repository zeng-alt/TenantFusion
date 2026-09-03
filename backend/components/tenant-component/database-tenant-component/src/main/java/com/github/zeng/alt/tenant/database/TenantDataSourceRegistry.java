package com.github.zeng.alt.tenant.database;

import com.github.zeng.alt.tenant.api.TenantRoutingException;
import com.github.zeng.alt.tenant.core.TenantProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库级隔离的数据源注册表。
 * <p>
 * 按需懒加载：租户数很多时预热全部连接池会瞬间吃掉大量连接，因此首次用到才建，
 * 建好后常驻并在容器关闭时统一释放。连接池默认 {@code minimumIdle = 0}，
 * 空闲租户不占用真实连接。
 * <p>
 * 凭据来自 {@code alt.tenant.datasources.<key>} 配置（口令由环境变量注入），
 * <b>不从租户元数据表读取</b>——避免又在库里堆明文口令。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class TenantDataSourceRegistry implements DisposableBean {

    private final TenantProperties properties;
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    public TenantDataSourceRegistry(TenantProperties properties) {
        this.properties = properties;
    }

    /**
     * 取（或首次创建）指定键的数据源。
     *
     * @param key 数据源键，即 {@code alt.tenant.datasources} 下的配置项名
     * @return 数据源
     * @throws TenantRoutingException 配置中不存在该键
     */
    public DataSource get(String key) {
        return dataSources.computeIfAbsent(key, this::create);
    }

    /**
     * 该键是否已配置，供启动期校验使用。
     *
     * @param key 数据源键
     * @return true 表示配置中存在
     */
    public boolean contains(String key) {
        return properties.getDatasources().containsKey(key);
    }

    @Override
    public void destroy() {
        dataSources.values().forEach(ds -> {
            if (ds instanceof HikariDataSource hikari) {
                hikari.close();
            }
        });
        dataSources.clear();
    }

    private DataSource create(String key) {
        TenantProperties.DataSourceConfig config = properties.getDatasources().get(key);
        if (config == null) {
            throw new TenantRoutingException(
                    "租户数据源键 [" + key + "] 未在 alt.tenant.datasources 下配置");
        }
        if (!StringUtils.hasText(config.getUrl())) {
            throw new TenantRoutingException("租户数据源 [" + key + "] 缺少 url");
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("tenant-" + key);
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        if (StringUtils.hasText(config.getDriverClassName())) {
            hikariConfig.setDriverClassName(config.getDriverClassName());
        }
        if (StringUtils.hasText(config.getSchema())) {
            hikariConfig.setSchema(config.getSchema());
        }
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        log.info("创建租户数据源 [" + key + "]，url=" + config.getUrl());
        return new HikariDataSource(hikariConfig);
    }
}
