package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataColumn;
import com.github.zeng.alt.security.data.DataColumnType;
import com.github.zeng.alt.security.data.DataResource;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 不完整或错误字段映射在启动时拒绝，避免遗漏过滤条件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionMappingTest {
    @Test
    void rejectsMissingAndDuplicatedAndInvalidColumns() {
        assertThatThrownBy(() -> metadata(Missing.class, Map.of())).hasStackTraceContaining("Missing data permission");
        assertThatThrownBy(() -> metadata(Duplicate.class, Map.of())).hasStackTraceContaining("Duplicate data permission");
        assertThatThrownBy(() -> metadata(Invalid.class, Map.of())).hasStackTraceContaining("Invalid data permission");
    }

    @Test
    void refusesQueryCacheForDelayedPermissionParameters() {
        assertThatThrownBy(() -> metadata(Missing.class, Map.of(
                "hibernate.cache.use_query_cache", true,
                "hibernate.cache.region.factory_class", NoCachingRegionFactory.INSTANCE
        )))
                .hasStackTraceContaining("cannot use query or entity second-level caches");
    }

    private void metadata(Class<?> entity, Map<String, Object> settings) {
        final var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySettings(settings).build();
        try {
            new MetadataSources(registry).addAnnotatedClass(entity).buildMetadata();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    /**
     * 校验夹具的实体基类。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @MappedSuperclass
    static class Base {
        @Id Long id;
        @DataColumn(DataColumnType.TENANT) String tenant;
        @DataColumn(DataColumnType.OWNER) Long owner;
    }

    /**
     * 缺少部门字段。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity @DataResource("missing")
    static class Missing extends Base { }

    /**
     * 重复拥有者字段。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity @DataResource("duplicate")
    static class Duplicate extends Base {
        @DataColumn(DataColumnType.OWNER) Long otherOwner;
        @DataColumn(DataColumnType.DEPARTMENT) Long department;
    }

    /**
     * 部门字段类型不匹配。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity @DataResource("invalid")
    static class Invalid extends Base {
        @DataColumn(DataColumnType.DEPARTMENT) String department;
    }
}
