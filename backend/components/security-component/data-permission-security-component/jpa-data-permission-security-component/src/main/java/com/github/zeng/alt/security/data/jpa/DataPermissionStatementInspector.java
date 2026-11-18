package com.github.zeng.alt.security.data.jpa;

import io.vavr.control.Option;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.security.access.AccessDeniedException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 保留已有 SQL 检查器，在权限查询作用域内拒绝写语句和未支持的语句类型。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionStatementInspector implements StatementInspector {
    private static final long serialVersionUID = 1L;
    private static final Pattern PREFIX_COMMENTS = Pattern.compile("(?s)^(?:\\s|/\\*.*?\\*/|--[^\\r\\n]*(?:\\r?\\n|$))*");
    private static final Pattern SELECT = Pattern.compile("^select\\b");
    private final StatementInspector delegate;

    DataPermissionStatementInspector(StatementInspector delegate) {
        this.delegate = delegate;
    }

    @Override
    public String inspect(String sql) {
        final String statement = Option.of(delegate).flatMap(inspector -> Option.of(inspector.inspect(sql))).getOrElse(sql);
        if (DataPermissionContext.active() && !DataPermissionContext.policyRead()) {
            final String normalized = PREFIX_COMMENTS.matcher(statement).replaceFirst("").toLowerCase(Locale.ROOT);
            if (!SELECT.matcher(normalized).find()) {
                throw new AccessDeniedException("@DataPermission methods are read-only; unsupported SQL statement");
            }
        }
        return statement;
    }
}
