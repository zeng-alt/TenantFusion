package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 配置历史的 JDBC 存取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class JdbcConfigHistoryRepository {
    private final JdbcConfigRepositoryHelper jdbc;

    ConfigHistoryDto create(ConfigHistoryDto history) {
        final long id = jdbc.create(
            """
                    INSERT INTO sys_config_history (config_id, content, version, operation, operator, created_date)
                    VALUES (:configId, :content, :version, :operation, :operator, COALESCE(:createdDate, CURRENT_TIMESTAMP))
                    """,
            history,
            "history_id"
        );
        return jdbc.get(
            "SELECT * FROM sys_config_history WHERE history_id = :id",
            Map.of("id", id),
            ConfigHistoryDto.class
        )
                .getOrElseThrow(() -> new ConfigException("历史写入后无法读取: " + id));
    }

    List<ConfigHistoryDto> list(Long configId) {
        return jdbc.list(
            "SELECT * FROM sys_config_history WHERE config_id = :id ORDER BY version DESC, history_id DESC",
            Map.of("id", configId),
            ConfigHistoryDto.class
        );
    }
}
