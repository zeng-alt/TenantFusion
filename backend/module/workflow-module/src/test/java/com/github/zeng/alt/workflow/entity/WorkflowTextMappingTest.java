package com.github.zeng.alt.workflow.entity;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工作流大文本字段的 PostgreSQL 建表兼容性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WorkflowTextMappingTest {

    @Test
    void generatesPostgresTextColumnsWithoutClob() {
        final var script = new StringWriter();
        final Map<String, Object> settings = Map.of(
                JdbcSettings.DIALECT, PostgreSQLDialect.class.getName(),
                JdbcSettings.ALLOW_METADATA_ON_BOOT, false,
                SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none",
                SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create",
                SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, script
        );
        try (final var registry = new StandardServiceRegistryBuilder().applySettings(settings).build()) {
            final var metadata = new MetadataSources(registry)
                    .addAnnotatedClass(FormDataEntity.class)
                    .addAnnotatedClass(FormFieldEntity.class)
                    .addAnnotatedClass(GlobalFormDataEntity.class)
                    .addAnnotatedClass(WorkflowVersionEntity.class)
                    .addAnnotatedClass(FormTemplateVersionEntity.class)
                    .buildMetadata();
            SchemaManagementToolCoordinator.process(
                    metadata, registry, settings, DelayedDropRegistryNotAvailableImpl.INSTANCE
            );
            assertThat(script.toString())
                    .contains("bpmn_xml text", "definition text", "data text", "validation_rules text",
                            "visibility_condition text", "field_props text")
                    .doesNotContainIgnoringCase("clob");
        }
    }
}
