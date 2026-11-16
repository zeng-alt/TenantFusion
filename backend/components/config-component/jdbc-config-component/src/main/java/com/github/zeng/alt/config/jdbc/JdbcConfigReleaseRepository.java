package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 发布快照的 JDBC 存取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class JdbcConfigReleaseRepository {
    private final JdbcConfigRepositoryHelper jdbc;

    ConfigReleaseDto create(ConfigReleaseDto release) {
        final long id = jdbc.create(
            """
                    INSERT INTO sys_config_release
                        (app_id, config_ids, release_version, release_note, status, snapshot, tenant_id,
                         created_by, created_date, last_modified_by, last_modified_date)
                    VALUES (:appId, :configIds, :releaseVersion, :releaseNote, COALESCE(:status, 'published'),
                            :snapshot, :tenantId, :createdBy, COALESCE(:createdDate, CURRENT_TIMESTAMP),
                            :lastModifiedBy, COALESCE(:lastModifiedDate, CURRENT_TIMESTAMP))
                    """,
            release,
            "release_id"
        );
        return get(id).getOrElseThrow(() -> new ConfigException("快照写入后无法读取: " + id));
    }

    ConfigReleaseDto update(ConfigReleaseDto release) {
        final int updated = jdbc.update(
            """
                    UPDATE sys_config_release
                    SET app_id = COALESCE(:appId, app_id), config_ids = COALESCE(:configIds, config_ids),
                        release_version = COALESCE(:releaseVersion, release_version),
                        release_note = COALESCE(:releaseNote, release_note), status = COALESCE(:status, status),
                        snapshot = COALESCE(:snapshot, snapshot), tenant_id = COALESCE(:tenantId, tenant_id),
                        last_modified_by = COALESCE(:lastModifiedBy, last_modified_by), last_modified_date = CURRENT_TIMESTAMP
                    WHERE release_id = :releaseId
                    """,
            release
        );
        if (updated == 0) {
            throw new ConfigException("发布记录不存在: " + release.releaseId());
        }
        return get(release.releaseId()).getOrElseThrow(() -> new ConfigException("快照更新后无法读取: " + release.releaseId()));
    }

    Option<ConfigReleaseDto> getLatest(Long appId) {
        return jdbc.get(
            "SELECT * FROM sys_config_release WHERE app_id = :id ORDER BY release_id DESC LIMIT 1",
            Map.of("id", appId),
            ConfigReleaseDto.class
        );
    }

    List<ConfigReleaseDto> listRecent(Long appId, int limit) {
        return jdbc.list(
            "SELECT * FROM sys_config_release WHERE app_id = :id ORDER BY release_id DESC LIMIT :limit",
            Map.of("id", appId, "limit", limit),
            ConfigReleaseDto.class
        );
    }

    private Option<ConfigReleaseDto> get(Long releaseId) {
        return jdbc.get(
            "SELECT * FROM sys_config_release WHERE release_id = :id",
            Map.of("id", releaseId),
            ConfigReleaseDto.class
        );
    }
}
