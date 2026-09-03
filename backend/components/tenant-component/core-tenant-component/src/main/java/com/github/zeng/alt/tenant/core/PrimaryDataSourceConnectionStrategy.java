package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantRouting;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 兜底连接来源：主数据源。
 * <p>
 * 行级 / 表级 / 模式级隔离都不换数据源，走的都是这条；只有库级隔离才由
 * {@code database-tenant-component} 提供优先级更高的实现。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class PrimaryDataSourceConnectionStrategy implements TenantConnectionStrategy {

    private final DataSource dataSource;

    public PrimaryDataSourceConnectionStrategy(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 兜底策略，接受一切路由 */
    @Override
    public boolean supports(TenantRouting routing) {
        return true;
    }

    @Override
    public Connection getConnection(TenantRouting routing) throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseConnection(TenantRouting routing, Connection connection) throws SQLException {
        connection.close();
    }

    /** 取最低优先级，确保任何专用策略都排在前面 */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 暴露底层数据源，供连接驱逐等场景使用。
     *
     * @return 主数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }
}
