package com.github.zeng.alt.tenant.api;

import org.springframework.core.Ordered;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接装饰器——在连接交给 Hibernate 前改写会话级状态，归还前复位。
 * <p>
 * 模式级隔离由此实现（{@code schema-tenant-component}）。与 {@link TenantConnectionStrategy} 分开，
 * 是为了让「独立库 + 库内非默认 schema」这种叠加组合不需要两个模块互相依赖。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantConnectionCustomizer extends Ordered {

    /**
     * 本装饰器是否适用于该路由。
     *
     * @param routing 租户路由
     * @return true 表示需要装饰
     */
    boolean supports(TenantRouting routing);

    /**
     * 应用租户相关的会话状态。
     *
     * @param connection 连接
     * @param routing    租户路由
     * @throws SQLException 应用失败
     */
    void apply(Connection connection, TenantRouting routing) throws SQLException;

    /**
     * 归还连接前复位会话状态。
     * <p>
     * <b>复位失败必须抛异常</b>：连接池会复用这条连接，带着上一个租户的 schema 还池会造成跨租户串数据，
     * 调用方据此销毁连接而不是还池。
     *
     * @param connection 连接
     * @param routing    租户路由
     * @throws SQLException 复位失败
     */
    void reset(Connection connection, TenantRouting routing) throws SQLException;

    @Override
    default int getOrder() {
        return 0;
    }
}
