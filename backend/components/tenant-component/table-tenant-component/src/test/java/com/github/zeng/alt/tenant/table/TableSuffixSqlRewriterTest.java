package com.github.zeng.alt.tenant.table;

import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.core.TenantProperties;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 表名重写的边界行为。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class TableSuffixSqlRewriterTest {

    private TableSuffixSqlRewriter rewriter;
    private TenantRouting routing;

    /** 元模型不可用的 ObjectProvider，逼迫注册表只用显式配置的表名 */
    private static final ObjectProvider<EntityManagerFactory> NO_EMF =
            new ObjectProvider<>() {
                @Override
                public EntityManagerFactory getObject(Object... args) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public EntityManagerFactory getObject() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public EntityManagerFactory getIfAvailable() {
                    return null;
                }

                @Override
                public EntityManagerFactory getIfUnique() {
                    return null;
                }
            };

    @BeforeEach
    void setUp() {
        TenantTableRegistry registry =
                new TenantTableRegistry(NO_EMF, Set.of("main_user", "main_role"));
        rewriter = new TableSuffixSqlRewriter(registry, new TenantProperties());
        routing = new TenantRouting("t001", null, null, "t001", false);
    }

    @Test
    @DisplayName("表名被追加租户后缀")
    void appendsSuffix() {
        assertThat(rewriter.rewrite("select u.id from main_user u", routing))
                .isEqualTo("select u.id from main_user_t001 u");
    }

    @Test
    @DisplayName("词边界：main_user 不应命中 main_user_role")
    void doesNotMatchLongerTableName() {
        String sql = "select * from main_user_role ur";
        // main_user_role 未登记，不应被改写；且不能因为它以 main_user 开头就被误伤
        assertThat(rewriter.rewrite(sql, routing)).isEqualTo(sql);
    }

    @Test
    @DisplayName("多个已登记表在同一条语句里都被改写")
    void rewritesMultipleTables() {
        assertThat(rewriter.rewrite(
                "select * from main_user u join main_role r on r.id = u.role_id", routing))
                .isEqualTo("select * from main_user_t001 u join main_role_t001 r on r.id = u.role_id");
    }

    @Test
    @DisplayName("字符串字面量里的表名不被改写")
    void skipsStringLiteral() {
        String sql = "select * from main_user where name = 'main_user'";
        assertThat(rewriter.rewrite(sql, routing))
                .isEqualTo("select * from main_user_t001 where name = 'main_user'");
    }

    @Test
    @DisplayName("未开启表级隔离时不介入")
    void inactiveWhenNotTableIsolated() {
        TenantRouting rowOnly = new TenantRouting("t001", null, null, null, true);
        assertThat(rewriter.supports(rowOnly)).isFalse();
    }

    @Test
    @DisplayName("没有登记任何表时不介入")
    void inactiveWithoutRegisteredTables() {
        TableSuffixSqlRewriter empty = new TableSuffixSqlRewriter(
                new TenantTableRegistry(NO_EMF, Set.of()), new TenantProperties());
        assertThat(empty.supports(routing)).isFalse();
    }
}
