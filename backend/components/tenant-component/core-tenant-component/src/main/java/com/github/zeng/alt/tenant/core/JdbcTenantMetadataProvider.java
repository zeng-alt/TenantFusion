package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 从元数据表读取租户路由信息。
 * <p>
 * 刻意用 {@link JdbcTemplate} 直连主数据源、绕开 Hibernate：租户元数据表自身不能被路由，
 * 否则「查元数据前先要知道租户」会形成死循环。表名由 {@code alt.tenant.metadata.table} 配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class JdbcTenantMetadataProvider implements TenantMetadataProvider {

    private final JdbcTemplate jdbcTemplate;
    private final String selectByIdSql;
    private final String selectAllSql;

    public JdbcTenantMetadataProvider(JdbcTemplate jdbcTemplate, TenantProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        // 表名来自配置而非用户输入，且在启动时固定；仍做一次标识符白名单校验
        String table = requireSimpleIdentifier(properties.getMetadata().getTable());
        String columns = """
                tenant_id, tenant_name, isolation_mode, datasource_key,
                schema_name, table_suffix, is_row_isolated, is_enabled
                """;
        this.selectByIdSql = "select " + columns + " from " + table
                + " where tenant_id = ? and is_enabled = true and (is_deleted = false or is_deleted is null)";
        this.selectAllSql = "select " + columns + " from " + table
                + " where is_enabled = true and (is_deleted = false or is_deleted is null)";
    }

    @Override
    public Optional<TenantMetadata> findById(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        List<TenantMetadata> rows = jdbcTemplate.query(selectByIdSql, ROW_MAPPER, tenantId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<TenantMetadata> findAll() {
        return jdbcTemplate.query(selectAllSql, ROW_MAPPER);
    }

    private static final RowMapper<TenantMetadata> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Boolean rowIsolated = rs.getObject("is_row_isolated") == null
                ? null
                : rs.getBoolean("is_row_isolated");
        return new TenantMetadata(
                rs.getString("tenant_id"),
                rs.getString("tenant_name"),
                TenantMode.of(rs.getString("isolation_mode")),
                rs.getString("datasource_key"),
                rs.getString("schema_name"),
                rs.getString("table_suffix"),
                rowIsolated,
                rs.getBoolean("is_enabled"));
    };

    private static String requireSimpleIdentifier(String table) {
        if (table == null || !table.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalStateException(
                    "alt.tenant.metadata.table 必须是简单标识符，当前值：" + table);
        }
        return table;
    }
}
