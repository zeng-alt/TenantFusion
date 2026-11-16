package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 配置应用的 JDBC 存取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class JdbcConfigAppRepository {
    private final JdbcConfigRepositoryHelper jdbc;

    ConfigAppDto create(ConfigAppDto app) {
        final long id = jdbc.create(
            """
                    INSERT INTO sys_config_app
                        (app_code, app_name, description, status, tenant_id, created_by, created_date,
                         last_modified_by, last_modified_date)
                    VALUES (:appCode, :appName, :description, COALESCE(:status, 1), :tenantId, :createdBy,
                            COALESCE(:createdDate, CURRENT_TIMESTAMP), :lastModifiedBy,
                            COALESCE(:lastModifiedDate, CURRENT_TIMESTAMP))
                    """,
            app,
            "app_id"
        );
        return get(id).getOrElseThrow(() -> new ConfigException("应用写入后无法读取: " + id));
    }

    Option<ConfigAppDto> get(Long appId) {
        return jdbc.get("SELECT * FROM sys_config_app WHERE app_id = :id", Map.of("id", appId), ConfigAppDto.class);
    }

    Option<ConfigAppDto> get(String appCode) {
        return jdbc.get(
            "SELECT * FROM sys_config_app WHERE app_code = :code",
            Map.of("code", appCode),
            ConfigAppDto.class
        );
    }

    List<ConfigAppDto> list() {
        return jdbc.list("SELECT * FROM sys_config_app ORDER BY app_id", Map.of(), ConfigAppDto.class);
    }
}
