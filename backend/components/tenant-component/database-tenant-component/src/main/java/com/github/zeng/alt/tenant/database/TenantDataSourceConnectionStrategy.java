package com.github.zeng.alt.tenant.database;

import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantRouting;
import org.springframework.core.Ordered;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 库级隔离：按路由的 {@code dataSourceKey} 取对应数据源的连接。
 * <p>
 * 优先级高于 core 的主数据源兜底策略，但只在 {@code dataSourceKey} 非空时接管，
 * 因此行级 / 表级 / 纯模式级的租户仍然走主数据源。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class TenantDataSourceConnectionStrategy implements TenantConnectionStrategy {

    private final TenantDataSourceRegistry registry;

    public TenantDataSourceConnectionStrategy(TenantDataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(TenantRouting routing) {
        return routing.isDatabaseIsolated();
    }

    @Override
    public Connection getConnection(TenantRouting routing) throws SQLException {
        return registry.get(routing.dataSourceKey()).getConnection();
    }

    @Override
    public void releaseConnection(TenantRouting routing, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
