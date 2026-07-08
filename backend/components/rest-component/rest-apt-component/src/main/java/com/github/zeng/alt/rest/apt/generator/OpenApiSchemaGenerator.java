package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.rest.apt.meta.QueryFieldMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.github.zeng.alt.rest.apt.util.TypeMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

/**
 * OpenAPI Schema 表达式生成器。
 * <p>集中构建 JavaPoet {@link CodeBlock} 形式的 OpenAPI Schema 表达式，</p>
 * 供 {@link RouterGenerator} 嵌入到自动生成的 {@code OpenApiCustomizer} 方法体中。
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public final class OpenApiSchemaGenerator {

    private static final ClassName SCHEMA =
            ClassName.get("io.swagger.v3.oas.models.media", "Schema");

    private OpenApiSchemaGenerator() {}

    /**
     * 构建带字段属性的对象 Schema 表达式：
     * <pre>{@code new Schema<>().type("object").description(name).addProperty("f1", ...)...}</pre>
     *
     * @param description Schema 的 description
     * @param fields      字段元数据列表
     */
    public static CodeBlock buildObjectSchemaWithFields(String description, List<SchemaFieldMeta> fields) {
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("new $T<>().type($S).description($S)", SCHEMA, "object", description);
        for (SchemaFieldMeta field : fields) {
            builder.add(".addProperty($S, ", field.getFieldName());
            builder.add(buildFieldSchema(field));
            builder.add(")");
        }
        return builder.build();
    }

    /**
     * 构建单个字段的 Schema 表达式：
     * <ul>
     *   <li>基本类型 → {@code new Schema<>().type("...").format("...")}</li>
     *   <li>实体引用 → {@code new Schema<>().$ref("#/components/schemas/...")}</li>
     * </ul>
     */
    public static CodeBlock buildFieldSchema(SchemaFieldMeta field) {
        String qualifiedName = field.getTypeQualifiedName();
        CodeBlock.Builder builder = CodeBlock.builder().add("new $T<>()", SCHEMA);

        if (TypeMapper.isSimpleScalarType(qualifiedName)) {
            builder.add(".type($S)", TypeMapper.toOpenApiType(qualifiedName));
            String format = TypeMapper.toOpenApiFormat(qualifiedName);
            if (format != null) {
                builder.add(".format($S)", format);
            }
            String desc = field.getDescription();
            if (!desc.isEmpty()) {
                builder.add(".description($S)", desc);
            }
        } else {
            builder.add(".$$ref($S)", "#/components/schemas/" + field.getTypeSimpleName());
        }

        return builder.build();
    }

    /**
     * 为 {@link QueryFieldMeta} 构建查询参数的 Schema 表达式。
     * 基本类型映射为具体 type/format，非标量类型使用空的 Schema。
     */
    public static CodeBlock buildQueryFieldSchema(QueryFieldMeta field) {
        String qualifiedName = field.getTypeQualifiedName();
        CodeBlock.Builder builder = CodeBlock.builder().add("new $T<>()", SCHEMA);

        if (TypeMapper.isSimpleScalarType(qualifiedName)) {
            builder.add(".type($S)", TypeMapper.toOpenApiType(qualifiedName));
            String format = TypeMapper.toOpenApiFormat(qualifiedName);
            if (format != null) {
                builder.add(".format($S)", format);
            }
        }

        return builder.build();
    }

    /**
     * 为空对象 Schema（无字段信息时使用）。
     * <pre>{@code new Schema<>().type("object").description(desc)}</pre>
     */
    public static CodeBlock emptyObjectSchema(String description) {
        return CodeBlock.builder()
                .add("new $T<>().type($S).description($S)", SCHEMA, "object", description)
                .build();
    }
}
