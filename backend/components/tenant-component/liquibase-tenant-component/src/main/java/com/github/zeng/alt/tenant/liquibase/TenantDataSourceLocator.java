package com.github.zeng.alt.tenant.liquibase;

import javax.sql.DataSource;

/**
 * 按数据源键取数据源。
 * <p>
 * 存在这层抽象，是为了让本模块不必依赖 {@code database-tenant-component}：
 * 只做模式级隔离的部署引入本模块时，不该被迫带上多数据源那一套。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@FunctionalInterface
public interface TenantDataSourceLocator {

    /**
     * 定位数据源。
     *
     * @param dataSourceKey 数据源键
     * @return 数据源
     */
    DataSource locate(String dataSourceKey);
}
