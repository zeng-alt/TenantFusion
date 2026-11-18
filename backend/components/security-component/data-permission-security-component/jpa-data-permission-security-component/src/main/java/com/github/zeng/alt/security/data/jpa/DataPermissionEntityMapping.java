package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataColumn;
import com.github.zeng.alt.security.data.DataColumnType;
import com.github.zeng.alt.security.data.DataResource;
import org.hibernate.MappingException;
import org.hibernate.annotations.TenantId;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

/**
 * 从 Hibernate 的实际属性映射解析数据权限列，支持继承与属性访问。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
record DataPermissionEntityMapping(
        Class<?> entity,
        String resource,
        ColumnDto tenant,
        ColumnDto owner,
        ColumnDto department
) {
    static DataPermissionEntityMapping from(PersistentClass binding, Dialect dialect) {
        final Class<?> entity = binding.getMappedClass();
        final String resource = entity.getAnnotation(DataResource.class).value();
        if (resource.isBlank() || binding.getJoinClosureSpan() != 0 || binding.isJoinedSubclass()) {
            throw new MappingException("Invalid or multi-table data permission resource: " + entity.getName());
        }
        final Map<DataColumnType, ColumnDto> columns = new EnumMap<>(DataColumnType.class);
        final var properties = new ArrayList<>(binding.getPropertyClosure());
        if (binding.getIdentifierProperty() != null) {
            properties.add(binding.getIdentifierProperty());
        }
        properties.forEach(property -> register(columns, binding, property, dialect));
        for (DataColumnType role : DataColumnType.values()) {
            if (!columns.containsKey(role)) {
                throw new MappingException("Missing data permission column " + role + " on " + entity.getName());
            }
        }
        return new DataPermissionEntityMapping(entity, resource, columns.get(DataColumnType.TENANT),
                columns.get(DataColumnType.OWNER), columns.get(DataColumnType.DEPARTMENT));
    }

    private static void register(
            Map<DataColumnType, ColumnDto> columns, PersistentClass binding, Property property, Dialect dialect
    ) {
        final var member = (AnnotatedElement) property.getGetter(binding.getMappedClass()).getMember();
        final DataColumn annotation = member.getAnnotation(DataColumn.class);
        if (annotation == null && !member.isAnnotationPresent(TenantId.class)) {
            return;
        }
        final DataColumnType role = annotation == null ? DataColumnType.TENANT : annotation.value();
        final Class<?> type = property.getType().getReturnedClass();
        final boolean valid = switch (role) {
            case TENANT -> type == String.class;
            case OWNER -> type == String.class || type == Long.class || type == long.class;
            case DEPARTMENT -> type == Long.class || type == long.class;
        };
        if (!valid || property.getColumnSpan() != 1 || property.getValue().hasFormula()
                || property.getValue().getTable() != binding.getTable()) {
            throw new MappingException("Invalid data permission column: " + property.getName());
        }
        final var column = new ColumnDto(property.getName(), property.getColumns().getFirst().getQuotedName(dialect),
                type, property == binding.getIdentifierProperty());
        if (columns.putIfAbsent(role, column) != null) {
            throw new MappingException("Duplicate data permission column: " + role);
        }
    }

    record ColumnDto(String property, String column, Class<?> type, boolean identifier) { }
}
