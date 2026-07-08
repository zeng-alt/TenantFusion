package com.github.zeng.alt.rest.apt.util;

/**
 * Java 类型 → OpenAPI 类型映射工具。
 * <p>集中管理所有 {@code isSimpleScalarType} / {@code mapTypeToOpenApi} / {@code mapTypeToFormat} 逻辑，</p>
 * 避免类型判断列表在多处重复维护。
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public final class TypeMapper {

    private TypeMapper() {}

    /**
     * 判断是否为 OpenAPI 可直接映射的简单标量类型。
     * 非标量类型（如自定义实体）应使用 {@code $ref} 引用。
     */
    public static boolean isSimpleScalarType(String qualifiedName) {
        return switch (qualifiedName) {
            case "java.lang.String", "java.lang.Integer", "java.lang.Long",
                 "java.lang.Boolean", "java.lang.Double", "java.lang.Float",
                 "java.lang.Short", "java.lang.Byte", "java.lang.Character",
                 "java.math.BigDecimal", "java.math.BigInteger",
                 "int", "long", "boolean", "double", "float", "short", "byte", "char",
                 "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
                 "java.time.OffsetDateTime", "java.util.Date" -> true;
            default -> false;
        };
    }

    /**
     * Java 类型 → OpenAPI {@code type}
     */
    public static String toOpenApiType(String qualifiedName) {
        return switch (qualifiedName) {
            case "java.lang.String" -> "string";
            case "java.lang.Integer", "java.lang.Short", "java.lang.Byte", "int", "short", "byte",
                 "java.lang.Long", "long" -> "integer";
            case "java.lang.Boolean", "boolean" -> "boolean";
            case "java.lang.Double", "double", "java.lang.Float", "float",
                 "java.math.BigDecimal", "java.math.BigInteger" -> "number";
            case "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
                 "java.time.OffsetDateTime", "java.util.Date" -> "string";
            default -> "string";
        };
    }

    /**
     * Java 类型 → OpenAPI {@code format}（部分类型需要），非标量返回 {@code null}。
     */
    public static String toOpenApiFormat(String qualifiedName) {
        return switch (qualifiedName) {
            case "java.lang.Integer", "int" -> "int32";
            case "java.lang.Short", "short" -> "int32";
            case "java.lang.Byte", "byte" -> "int32";
            case "java.lang.Long", "long" -> "int64";
            case "java.lang.Float", "float" -> "float";
            case "java.lang.Double", "double" -> "double";
            case "java.time.LocalDate" -> "date";
            case "java.time.LocalDateTime" -> "date-time";
            case "java.time.OffsetDateTime" -> "date-time";
            default -> null;
        };
    }
}
