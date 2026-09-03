package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingException;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import javax.sql.DataSource;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hibernate 唯一的 {@code MultiTenantConnectionProvider}（{@code hibernate.multi_tenant_connection_provider}）。
 * <p>
 * 库级与模式级隔离共用这一个 Hibernate SPI，所以它必须由 core 独占，再向下分发：
 * <ul>
 *   <li>{@link TenantConnectionStrategy} 决定「从哪个数据源取连接」——库级隔离在此生效</li>
 *   <li>{@link TenantConnectionCustomizer} 决定「拿到连接后改什么会话状态」——模式级隔离在此生效</li>
 * </ul>
 * 两者分开，使「独立库 + 库内非默认 schema」这种叠加组合不需要两个策略模块互相依赖。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class RoutingMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient TenantRoutingRegistry registry;
    private final transient List<TenantConnectionStrategy> strategies;
    private final transient List<TenantConnectionCustomizer> customizers;
    private final transient DataSource anyDataSource;
    private final transient String anyTenantId;

    public RoutingMultiTenantConnectionProvider(
            TenantRoutingRegistry registry,
            List<TenantConnectionStrategy> strategies,
            List<TenantConnectionCustomizer> customizers,
            DataSource anyDataSource,
            String anyTenantId) {
        this.registry = registry;
        this.strategies = strategies;
        this.customizers = customizers;
        this.anyDataSource = anyDataSource;
        this.anyTenantId = anyTenantId;
    }

    /**
     * Hibernate 启动做 schema 校验、以及任何还没有租户上下文的时刻会调这里。
     * <p>
     * 库级部署下必须能返回一条连接，否则启动阶段就会失败——所以固定走主数据源，
     * 并配合 {@code hibernate.multi_tenant.datasource.identifier_for_any} 指向默认租户。
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        return anyDataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        TenantRouting routing = registry.resolve(tenantIdentifier);
        TenantConnectionStrategy strategy = selectStrategy(routing);
        Connection connection = strategy.getConnection(routing);
        try {
            for (TenantConnectionCustomizer customizer : customizers) {
                if (customizer.supports(routing)) {
                    customizer.apply(connection, routing);
                }
            }
        } catch (SQLException | RuntimeException e) {
            // 装饰失败的连接状态未知，直接关掉而不是交给 Hibernate
            closeQuietly(connection);
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        TenantRouting routing = registry.resolve(tenantIdentifier);
        boolean resetFailed = false;
        List<TenantConnectionCustomizer> reversed = new ArrayList<>(customizers);
        Collections.reverse(reversed);
        for (TenantConnectionCustomizer customizer : reversed) {
            if (!customizer.supports(routing)) {
                continue;
            }
            try {
                customizer.reset(connection, routing);
            } catch (SQLException | RuntimeException e) {
                resetFailed = true;
                log.error("租户 [" + routing.tenantId() + "] 连接复位失败，将销毁该连接以避免跨租户串数据", e);
            }
        }
        if (resetFailed) {
            // 复位失败的连接绝不能还池：下一个租户会拿到带着上一个租户 schema 的脏连接
            evict(connection);
            throw new TenantRoutingException(
                    "租户 [" + routing.tenantId() + "] 的连接复位失败，已销毁连接");
        }
        selectStrategy(routing).releaseConnection(routing, connection);
    }

    /**
     * 返回 false。开启激进释放会让 Hibernate 在每条语句后归还连接，
     * 而模式级隔离每次取连接都要执行一次 {@code SET SCHEMA}，代价过高。
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isAssignableFrom(getClass())
                || unwrapType.isAssignableFrom(MultiTenantConnectionProvider.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new TenantRoutingException("无法解包为 " + unwrapType.getName());
    }

    /** 供启动期校验读取 */
    String anyTenantId() {
        return anyTenantId;
    }

    private TenantConnectionStrategy selectStrategy(TenantRouting routing) {
        for (TenantConnectionStrategy strategy : strategies) {
            if (strategy.supports(routing)) {
                return strategy;
            }
        }
        throw new TenantRoutingException(
                "租户 [" + routing.tenantId() + "] 没有可用的连接来源策略，"
                        + "库级隔离需要引入 database-tenant-component");
    }

    private void evict(Connection connection) {
        // Hikari 提供 evictConnection 用于丢弃而非归还；反射调用以免 core 强依赖具体连接池
        try {
            anyDataSource.getClass()
                    .getMethod("evictConnection", Connection.class)
                    .invoke(anyDataSource, connection);
            return;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 连接池不支持驱逐，退化为直接关闭
        }
        closeQuietly(connection);
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("关闭连接失败", e);
        }
    }
}
