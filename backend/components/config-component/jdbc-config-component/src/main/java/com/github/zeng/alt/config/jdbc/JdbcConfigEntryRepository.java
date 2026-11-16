package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * 配置条目的 JDBC 存取，使用 version 保持与 JPA 相同的版本更新语义。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class JdbcConfigEntryRepository {
    private static final String SELECT = """
            SELECT config_id, app_id, data_id, group_name AS "group", content, format, description,
                   version, status, tenant_id, created_by, created_date, last_modified_by, last_modified_date
            FROM sys_config_info
            """;
    private final JdbcConfigRepositoryHelper jdbc;

    ConfigEntryDto create(ConfigEntryDto config) {
        final long id = jdbc.create(
            """
                    INSERT INTO sys_config_info
                        (app_id, data_id, group_name, content, format, description, version, status, tenant_id,
                         created_by, created_date, last_modified_by, last_modified_date)
                    VALUES (:appId, :dataId, COALESCE(:group, 'DEFAULT_GROUP'), :content,
                            COALESCE(:format, 'properties'), :description, COALESCE(:version, 0),
                            COALESCE(:status, 'draft'), :tenantId, :createdBy,
                            COALESCE(:createdDate, CURRENT_TIMESTAMP), :lastModifiedBy,
                            COALESCE(:lastModifiedDate, CURRENT_TIMESTAMP))
                    """,
            config,
            "config_id"
        );
        return get(id).getOrElseThrow(() -> new ConfigException("配置写入后无法读取: " + id));
    }

    Option<ConfigEntryDto> get(Long configId) {
        return jdbc.get(SELECT + " WHERE config_id = :id", Map.of("id", configId), ConfigEntryDto.class);
    }

    Option<ConfigEntryDto> get(Long appId, String dataId, String group) {
        return jdbc.get(
            SELECT + " WHERE app_id = :app AND data_id = :key AND group_name = :group",
            Map.of("app", appId, "key", dataId, "group", group),
            ConfigEntryDto.class
        );
    }

    List<ConfigEntryDto> list(Long appId) {
        return jdbc.list(SELECT + " WHERE app_id = :app ORDER BY config_id", Map.of("app", appId), ConfigEntryDto.class);
    }

    List<ConfigEntryDto> list(List<Long> configIds) {
        if (configIds.isEmpty()) {
            return List.of();
        }
        return jdbc.list(
            SELECT + " WHERE config_id IN (:ids) ORDER BY config_id",
            Map.of("ids", configIds),
            ConfigEntryDto.class
        );
    }

    ConfigEntryDto update(ConfigEntryDto config) {
        final int updated = jdbc.update(
            """
                    UPDATE sys_config_info
                    SET app_id = COALESCE(:appId, app_id), data_id = COALESCE(:dataId, data_id),
                        group_name = COALESCE(:group, group_name), content = COALESCE(:content, content),
                        format = COALESCE(:format, format), description = COALESCE(:description, description),
                        status = COALESCE(:status, status), tenant_id = COALESCE(:tenantId, tenant_id),
                        last_modified_by = COALESCE(:lastModifiedBy, last_modified_by),
                        last_modified_date = CURRENT_TIMESTAMP, version = version + 1
                    WHERE config_id = :configId AND version = :version
                    """,
            config
        );
        if (updated == 0) {
            throw new OptimisticLockingFailureException("配置不存在或版本已更新: " + config.configId());
        }
        return get(config.configId()).getOrElseThrow(() -> new ConfigException("配置更新后无法读取: " + config.configId()));
    }

    Option<ConfigEntryDto> delete(Long configId) {
        return get(configId).map(config -> update(config.toBuilder().status("deleted").build()));
    }
}
