package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * JDBC 结果边界和生成主键处理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class JdbcConfigRepositoryHelper {
    private final JdbcClient jdbc;

    <T> Option<T> get(String sql, Map<String, ?> parameters, Class<T> type) {
        return Option.ofOptional(jdbc.sql(sql).params(parameters).query(type).optional());
    }

    <T> List<T> list(String sql, Map<String, ?> parameters, Class<T> type) {
        return List.copyOf(jdbc.sql(sql).params(parameters).query(type).list());
    }

    long create(String sql, Object model, String keyColumn) {
        final var keys = new GeneratedKeyHolder();
        jdbc.sql(sql).paramSource(model).update(keys, keyColumn);
        return Option.of(keys.getKey()).map(Number::longValue)
                .getOrElseThrow(() -> new ConfigException("存储未返回生成的主键: " + keyColumn));
    }

    int update(String sql, Object model) {
        return jdbc.sql(sql).paramSource(model).update();
    }
}
