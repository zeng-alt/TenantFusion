package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

/**
 * 本地租户库直连的元数据来源。
 * <p>
 * 语义与 {@code core} 里单库回退的 {@code JdbcTenantMetadataProvider} 一致，
 * 区别是强制绑定具名的租户库 {@code JdbcTemplate}，而不是主业务数据源。
 * 租户库自身不受任何隔离（无 {@code @TenantId}、无连接路由），不会形成自举死循环。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class LocalTenantMetadataProvider implements TenantMetadataProvider {

    private final JdbcTemplate jdbcTemplate;
    private final String selectByIdSql;
    private final String selectAllSql;

    public LocalTenantMetadataProvider(JdbcTemplate jdbcTemplate, String table) {
        this.jdbcTemplate = jdbcTemplate;
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
}
