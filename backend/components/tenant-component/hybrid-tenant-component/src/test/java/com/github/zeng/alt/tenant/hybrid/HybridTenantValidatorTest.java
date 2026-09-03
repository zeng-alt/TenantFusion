package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import com.github.zeng.alt.tenant.core.PrimaryDataSourceConnectionStrategy;
import com.github.zeng.alt.tenant.core.TenantProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 混合模式的启动期快速失败。
 * <p>
 * 这是混合模式独有的故障形态：租户声明了某档位隔离，但对应策略模块没被引入。
 * 此时请求会静默落到主数据源——看起来能跑，实际上串了别的租户的数据。
 * 单档位部署不会有这个问题，因为档位由配置固定、模块是否在场一目了然。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class HybridTenantValidatorTest {

    private static final TenantMetadata DB_TENANT = new TenantMetadata(
            "big", "大客户", TenantMode.DATABASE, "ds_big", null, null, null, true);
    private static final TenantMetadata SCHEMA_TENANT = new TenantMetadata(
            "mid", "中客户", TenantMode.SCHEMA, null, "sch_mid", null, null, true);
    private static final TenantMetadata TABLE_TENANT = new TenantMetadata(
            "tbl", "表级客户", TenantMode.TABLE, null, null, "t001", null, true);

    @Test
    @DisplayName("租户声明库级隔离但缺 database 模块：启动即失败")
    void failsWhenDatabaseModuleMissing() {
        HybridTenantValidator validator = validator(DB_TENANT,
                // 只有 core 的主数据源兜底策略，没有真正的库级策略
                ListObjectProvider.of(fallbackStrategy()),
                ListObjectProvider.of(),
                ListObjectProvider.of());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("big")
                .hasMessageContaining("database-tenant-component");
    }

    @Test
    @DisplayName("租户声明模式级隔离但缺 schema 模块：启动即失败")
    void failsWhenSchemaModuleMissing() {
        HybridTenantValidator validator = validator(SCHEMA_TENANT,
                ListObjectProvider.of(fallbackStrategy()),
                ListObjectProvider.of(),
                ListObjectProvider.of());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema-tenant-component");
    }

    @Test
    @DisplayName("租户声明表级隔离但缺 table 模块：启动即失败")
    void failsWhenTableModuleMissing() {
        HybridTenantValidator validator = validator(TABLE_TENANT,
                ListObjectProvider.of(fallbackStrategy()),
                ListObjectProvider.of(),
                ListObjectProvider.of());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("table-tenant-component");
    }

    @Test
    @DisplayName("模块齐备时校验通过")
    void passesWhenAllModulesPresent() {
        HybridTenantValidator validator = validator(DB_TENANT,
                ListObjectProvider.of(fallbackStrategy(), databaseStrategy()),
                ListObjectProvider.of(schemaCustomizer()),
                ListObjectProvider.of(tableRewriter()));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("错误信息一次列出所有问题租户，而不是只报第一个")
    void reportsAllProblems() {
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                new TenantProperties(),
                TenantMetadataProviderStub.of(DB_TENANT, SCHEMA_TENANT, TABLE_TENANT));
        HybridTenantValidator validator = new HybridTenantValidator(
                registry,
                TenantMetadataProviderStub.of(DB_TENANT, SCHEMA_TENANT, TABLE_TENANT),
                ListObjectProvider.of(fallbackStrategy()),
                ListObjectProvider.of(),
                ListObjectProvider.of());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .contains("big").contains("mid").contains("tbl"));
    }

    private HybridTenantValidator validator(
            TenantMetadata tenant,
            ObjectProvider<TenantConnectionStrategy> strategies,
            ObjectProvider<TenantConnectionCustomizer> customizers,
            ObjectProvider<TenantSqlRewriter> rewriters) {
        HybridTenantRoutingRegistry registry = new HybridTenantRoutingRegistry(
                new TenantProperties(), TenantMetadataProviderStub.of(tenant));
        return new HybridTenantValidator(
                registry, TenantMetadataProviderStub.of(tenant),
                strategies, customizers, rewriters);
    }

    /** core 的兜底策略：对一切路由都返回 true，校验器必须把它识别出来并排除 */
    private TenantConnectionStrategy fallbackStrategy() {
        return new PrimaryDataSourceConnectionStrategy(null);
    }

    private TenantConnectionStrategy databaseStrategy() {
        return new TenantConnectionStrategy() {
            @Override
            public boolean supports(TenantRouting routing) {
                return routing.isDatabaseIsolated();
            }

            @Override
            public Connection getConnection(TenantRouting routing) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseConnection(TenantRouting routing, Connection connection) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private TenantConnectionCustomizer schemaCustomizer() {
        return new TenantConnectionCustomizer() {
            @Override
            public boolean supports(TenantRouting routing) {
                return routing.isSchemaIsolated();
            }

            @Override
            public void apply(Connection connection, TenantRouting routing) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void reset(Connection connection, TenantRouting routing) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private TenantSqlRewriter tableRewriter() {
        return new TenantSqlRewriter() {
            @Override
            public boolean supports(TenantRouting routing) {
                return routing.isTableIsolated();
            }

            @Override
            public String rewrite(String sql, TenantRouting routing) {
                return sql;
            }
        };
    }
}
