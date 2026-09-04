package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingException;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 逐租户执行 Liquibase 迁移。
 * <p>
 * Spring Boot 自带的 {@code SpringLiquibase} 只会迁移主数据源的默认 schema，
 * 那对行级和表级隔离够用——它们共享同一份表结构。但模式级和库级隔离下，
 * 每个租户各有一套物理表，必须逐个迁移，否则新增字段只落在主 schema 上。
 * <p>
 * <b>表级隔离不在本类覆盖范围内</b>：那需要 changelog 里的表名本身参数化
 * （{@code main_user_${tenantSuffix}}），是 changelog 的写法问题，不是执行器能补的。
 * 遇到表级租户会记一条告警。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@CommonsLog
public class TenantLiquibaseRunner implements InitializingBean {

    private final TenantLiquibaseProperties properties;
    private final TenantMetadataProvider metadataProvider;
    private final TenantRoutingRegistry registry;
    private final DataSource defaultDataSource;
    private final ObjectProvider<TenantDialect> dialect;
    private final ResourceLoader resourceLoader;
    private final TenantDataSourceLocator dataSourceLocator;

    public TenantLiquibaseRunner(
            TenantLiquibaseProperties properties,
            TenantMetadataProvider metadataProvider,
            TenantRoutingRegistry registry,
            DataSource defaultDataSource,
            ObjectProvider<TenantDialect> dialect,
            ResourceLoader resourceLoader,
            TenantDataSourceLocator dataSourceLocator) {
        this.properties = properties;
        this.metadataProvider = metadataProvider;
        this.registry = registry;
        this.defaultDataSource = defaultDataSource;
        this.dialect = dialect;
        this.resourceLoader = resourceLoader;
        this.dataSourceLocator = dataSourceLocator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        migrateAll();
    }

    /**
     * 遍历启用中的租户并执行迁移。
     *
     * @throws Exception 任一租户失败且 {@code failFast} 为真
     */
    public void migrateAll() throws Exception {
        List<TenantMetadata> tenants = metadataProvider.findAll();
        List<String> failures = new ArrayList<>();
        int migrated = 0;

        for (TenantMetadata tenant : tenants) {
            TenantRouting routing = registry.resolve(tenant.tenantId());
            if (!needsOwnMigration(routing)) {
                continue;
            }
            try {
                migrate(routing);
                migrated++;
            } catch (Exception e) {
                if (Boolean.TRUE.equals(properties.getFailFast())) {
                    throw new TenantRoutingException(
                            "租户 [" + routing.tenantId() + "] 迁移失败", e);
                }
                log.error("租户 [" + routing.tenantId() + "] 迁移失败，已跳过", e);
                failures.add(routing.tenantId());
            }
        }
        log.info("多租户迁移完成：成功 " + migrated + " 个"
                + (failures.isEmpty() ? "" : "，失败 " + failures));
    }

    /**
     * 只有独立 schema 或独立库的租户需要单独迁移。
     * <p>行级与表级租户共享主 schema，已由 Spring 的主迁移覆盖。
     */
    private boolean needsOwnMigration(TenantRouting routing) {
        if (routing.isTableIsolated()) {
            log.warn("租户 [" + routing.tenantId() + "] 使用表级隔离，本执行器不予处理："
                    + "表级隔离需要 changelog 里的表名自身参数化，请另行维护");
        }
        return routing.isSchemaIsolated() || routing.isDatabaseIsolated();
    }

    private void migrate(TenantRouting routing) throws Exception {
        DataSource target = routing.isDatabaseIsolated()
                ? dataSourceLocator.locate(routing.dataSourceKey())
                : defaultDataSource;

        if (routing.isSchemaIsolated() && Boolean.TRUE.equals(properties.getCreateSchema())) {
            createSchema(target, routing.schemaName());
        }

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog(properties.getChangeLog());
        if (routing.isSchemaIsolated()) {
            // setDefaultSchema 只管 Liquibase 自己生成的变更和变更记录表的位置，
            // 对 formatted SQL 里的原生 CREATE TABLE 无效，所以还得把 schema 设到连接上
            liquibase.setDefaultSchema(routing.schemaName());
            liquibase.setLiquibaseSchema(routing.schemaName());
            liquibase.setDataSource(new SchemaAwareDataSource(
                    target, requireDialect(), routing.schemaName()));
        } else {
            liquibase.setDataSource(target);
        }
        if (StringUtils.hasText(properties.getContexts())) {
            liquibase.setContexts(properties.getContexts());
        }
        if (StringUtils.hasText(properties.getLabels())) {
            liquibase.setLabelFilter(properties.getLabels());
        }
        liquibase.setChangeLogParameters(parametersFor(routing));

        log.info("开始迁移租户 [" + routing.tenantId() + "]，目标 "
                + (routing.isDatabaseIsolated() ? "库=" + routing.dataSourceKey() : "schema=" + routing.schemaName()));
        liquibase.afterPropertiesSet();
    }

    private Map<String, String> parametersFor(TenantRouting routing) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(properties.getPassTenantNameParameter())) {
            parameters.put("tenantName", routing.tenantId());
        }
        if (routing.schemaName() != null) {
            parameters.put("tenantSchema", routing.schemaName());
        }
        if (routing.tableSuffix() != null) {
            parameters.put("tenantSuffix", routing.tableSuffix());
        }
        return parameters;
    }

    private void createSchema(DataSource dataSource, String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(requireDialect().createSchemaSql(schema));
        }
    }

    /**
     * 取方言，缺失时给出可操作的错误。
     *
     * @return 方言
     */
    private TenantDialect requireDialect() {
        TenantDialect current = dialect.getIfAvailable();
        if (current == null) {
            throw new TenantRoutingException(
                    "模式级隔离的迁移需要 TenantDialect，请引入 h2-tenant-component "
                            + "或 pg-tenant-component");
        }
        return current;
    }
}
