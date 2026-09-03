package com.github.zeng.alt.tenant.api;

import org.springframework.core.Ordered;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接来源策略——决定「从哪个数据源取连接」。
 * <p>
 * 只有库级隔离需要自定义实现（{@code database-tenant-component}）；模式级隔离不换数据源，
 * 只是拿到连接后改 schema，因此由 {@link TenantConnectionCustomizer} 表达而非本接口。
 * core 模块始终注册一个使用主数据源、优先级最低的兜底实现。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantConnectionStrategy extends Ordered {

    /**
     * 本策略是否适用于该路由。
     *
     * @param routing 租户路由
     * @return true 表示由本策略提供连接
     */
    boolean supports(TenantRouting routing);

    /**
     * 取连接。
     *
     * @param routing 租户路由
     * @return 数据库连接
     * @throws SQLException 取连接失败
     */
    Connection getConnection(TenantRouting routing) throws SQLException;

    /**
     * 归还连接。
     *
     * @param routing    租户路由
     * @param connection 待归还连接
     * @throws SQLException 归还失败
     */
    void releaseConnection(TenantRouting routing, Connection connection) throws SQLException;

    @Override
    default int getOrder() {
        return 0;
    }
}
