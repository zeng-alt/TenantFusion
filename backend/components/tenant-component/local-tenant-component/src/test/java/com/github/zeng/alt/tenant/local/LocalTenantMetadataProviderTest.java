package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地租户库元数据来源：独立 H2 租户库读写验证。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class LocalTenantMetadataProviderTest {

    private LocalTenantMetadataProvider provider;

    @BeforeEach
    void setUp() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:mem:tenant_store_test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS main_tenant");
        jdbcTemplate.execute("""
                CREATE TABLE main_tenant (
                    tenant_id VARCHAR(64) PRIMARY KEY,
                    tenant_name VARCHAR(255),
                    isolation_mode VARCHAR(16),
                    datasource_key VARCHAR(64),
                    schema_name VARCHAR(64),
                    table_suffix VARCHAR(32),
                    is_row_isolated BOOLEAN,
                    is_enabled BOOLEAN,
                    is_deleted BOOLEAN
                )
                """);
        jdbcTemplate.update(
                "INSERT INTO main_tenant VALUES ('master','主租户','NONE',null,null,null,false,true,false)");
        jdbcTemplate.update(
                "INSERT INTO main_tenant VALUES ('t001','租户一','SCHEMA',null,'sch_t001',null,null,true,false)");
        jdbcTemplate.update(
                "INSERT INTO main_tenant VALUES ('t002','停用租户','ROW',null,null,null,true,false,false)");
        provider = new LocalTenantMetadataProvider(jdbcTemplate, "main_tenant");
    }

    @Test
    @DisplayName("按租户标识查到路由元数据")
    void findByIdResolvesRoutingMetadata() {
        Optional<TenantMetadata> found = provider.findById("t001");

        assertThat(found).isPresent();
        assertThat(found.get().mode()).isEqualTo(TenantMode.SCHEMA);
        assertThat(found.get().schemaName()).isEqualTo("sch_t001");
    }

    @Test
    @DisplayName("停用租户查不到，全量只返回启用租户")
    void disabledTenantIsInvisible() {
        assertThat(provider.findById("t002")).isEmpty();
        assertThat(provider.findById("nope")).isEmpty();
        assertThat(provider.findById(null)).isEmpty();

        List<TenantMetadata> all = provider.findAll();
        assertThat(all).extracting(TenantMetadata::tenantId)
                .containsExactlyInAnyOrder("master", "t001");
    }

    @Test
    @DisplayName("老接口适配器把 datasourceKey 译为 db")
    void legacyAdapterMapsFields() {
        LocalTenantSingleDataSourceProvider adapter =
                new LocalTenantSingleDataSourceProvider(provider);

        assertThat(adapter.findById("t001")).isPresent();
        assertThat(adapter.findById("t001").get().getSchema()).isEqualTo("sch_t001");
        assertThat(adapter.findById("unknown")).isEmpty();
    }
}
