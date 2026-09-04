package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * 把每条连接的当前 schema 固定到指定值的数据源装饰器。
 * <p>
 * <b>为什么必须有这一层</b>：{@code SpringLiquibase#setDefaultSchema} 只影响 Liquibase
 * <i>自己生成</i>的变更（YAML/XML 里的 {@code createTable} 之类）以及变更记录表的位置，
 * 对 formatted SQL changelog 里的原生 {@code CREATE TABLE xxx} 完全不起作用——那条 SQL
 * 会原样发给数据库，落在连接的当前 schema 上。
 * <p>
 * 本项目的 changelog 全是原生 SQL，若只设 {@code defaultSchema}，所有租户的表都会建到
 * 默认 schema 里：第一个租户"迁移成功"，第二个租户报表已存在，而且没人能一眼看出原因。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
record SchemaAwareDataSource(DataSource delegate, TenantDialect dialect, String schema)
        implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return applySchema(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return applySchema(delegate.getConnection(username, password));
    }

    private Connection applySchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.schemaSwitchSql(schema));
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
