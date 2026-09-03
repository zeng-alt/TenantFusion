package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.io.Serial;
import java.util.List;

/**
 * Hibernate 唯一的 {@code StatementInspector}（{@code hibernate.session_factory.statement_inspector}），
 * 把多个 {@link TenantSqlRewriter} 串起来。
 * <p>
 * {@code inspect} 只拿到 SQL 字符串、拿不到租户，只能从 {@link TenantContextTracker} 读当前路由。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class CompositeTenantStatementInspector implements StatementInspector {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<TenantSqlRewriter> rewriters;

    public CompositeTenantStatementInspector(List<TenantSqlRewriter> rewriters) {
        this.rewriters = rewriters;
    }

    @Override
    public String inspect(String sql) {
        if (sql == null || rewriters.isEmpty()) {
            return sql;
        }
        TenantRouting routing = TenantContextTracker.currentRouting();
        if (routing == null) {
            return sql;
        }
        String result = sql;
        for (TenantSqlRewriter rewriter : rewriters) {
            if (rewriter.supports(routing)) {
                result = rewriter.rewrite(result, routing);
            }
        }
        return result;
    }
}
