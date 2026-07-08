package com.github.zeng.alt.rest.apt.meta;

import java.util.Objects;

/**
 * Schema 字段元模型 — 描述实体字段信息，用于生成 OpenAPI Schema 属性。
 * <p>不可变值对象，通过静态工厂 {@link #of(String, String, String, String)} 创建。</p>
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 2.0
 */
public final class SchemaFieldMeta {

    private final String fieldName;
    private final String typeQualifiedName;
    private final String typeSimpleName;
    private final String description;

    private SchemaFieldMeta(String fieldName, String typeQualifiedName,
                            String typeSimpleName, String description) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.typeQualifiedName = Objects.requireNonNull(typeQualifiedName, "typeQualifiedName");
        this.typeSimpleName = typeSimpleName != null ? typeSimpleName : "";
        this.description = description != null ? description : "";
    }

    /**
     * 静态工厂，取代 Builder 模式
     */
    public static SchemaFieldMeta of(String fieldName, String typeQualifiedName,
                                     String typeSimpleName, String description) {
        return new SchemaFieldMeta(fieldName, typeQualifiedName, typeSimpleName, description);
    }

    public String getFieldName() { return fieldName; }

    public String getTypeQualifiedName() { return typeQualifiedName; }

    public String getTypeSimpleName() { return typeSimpleName; }

    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaFieldMeta that)) return false;
        return fieldName.equals(that.fieldName)
                && typeQualifiedName.equals(that.typeQualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, typeQualifiedName);
    }

    @Override
    public String toString() {
        return "SchemaFieldMeta{" + fieldName + ": " + typeQualifiedName + "}";
    }
}
