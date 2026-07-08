package com.github.zeng.alt.rest.apt.meta;

import com.github.zeng.alt.rest.annotation.QueryType;

/**
 * 查询字段元模型 — 描述单个查询字段的注解信息
 *
 * @author zengJiaJun
 * @since 2026年07月06日
 * @version 1.0
 */
public class QueryFieldMeta {

    private final String fieldName;
    private final String column;
    private final QueryType queryType;
    private final boolean ignoreNull;
    private final boolean multi;
    private final String rangeStart;
    private final String rangeEnd;
    private final boolean hasOrder;
    private final boolean orderAsc;
    private final int orderPriority;
    private final boolean autoSort;
    private final String typeQualifiedName;
    private final String typeSimpleName;
    private final String description;

    private QueryFieldMeta(Builder builder) {
        this.fieldName = builder.fieldName;
        this.column = builder.column;
        this.queryType = builder.queryType;
        this.ignoreNull = builder.ignoreNull;
        this.multi = builder.multi;
        this.rangeStart = builder.rangeStart;
        this.rangeEnd = builder.rangeEnd;
        this.hasOrder = builder.hasOrder;
        this.orderAsc = builder.orderAsc;
        this.orderPriority = builder.orderPriority;
        this.autoSort = builder.autoSort;
        this.typeQualifiedName = builder.typeQualifiedName;
        this.typeSimpleName = builder.typeSimpleName;
        this.description = builder.description;
    }

    public String getFieldName() { return fieldName; }

    public String getColumn() { return column; }

    public QueryType getQueryType() { return queryType; }

    public boolean isIgnoreNull() { return ignoreNull; }

    public boolean isMulti() { return multi; }

    public String getRangeStart() { return rangeStart; }

    public String getRangeEnd() { return rangeEnd; }

    public boolean isHasOrder() { return hasOrder; }

    public boolean isOrderAsc() { return orderAsc; }

    public int getOrderPriority() { return orderPriority; }

    public boolean isAutoSort() { return autoSort; }

    public String getTypeQualifiedName() { return typeQualifiedName; }

    public String getTypeSimpleName() { return typeSimpleName; }

    public boolean isStringType() {
        return "java.lang.String".equals(typeQualifiedName) || "String".equals(typeSimpleName);
    }

    public boolean isNumberType() {
        return switch (typeQualifiedName) {
            case "java.lang.Integer", "java.lang.Long", "java.lang.Double",
                 "java.math.BigDecimal", "int", "long", "double" -> true;
            default -> false;
        };
    }

    public boolean isBooleanType() {
        return "java.lang.Boolean".equals(typeQualifiedName) || "boolean".equals(typeQualifiedName);
    }

    public String getDescription() {
        return description;
    }

    public boolean isTemporalType() {
        return switch (typeQualifiedName) {
            case "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime" -> true;
            default -> false;
        };
    }

    public boolean isRange() {
        return queryType == QueryType.BETWEEN || !rangeStart.isEmpty() || !rangeEnd.isEmpty();
    }

    /**
     * 获取值转换表达式，用于将 String 类型的请求参数转换为目标类型
     */
    public String getConversionExpr(String paramVarName) {
        return switch (typeQualifiedName) {
            case "java.lang.String" -> paramVarName;
            case "java.lang.Integer", "int" -> "Integer.parseInt(" + paramVarName + ")";
            case "java.lang.Long", "long" -> "Long.parseLong(" + paramVarName + ")";
            case "java.lang.Double", "double" -> "Double.parseDouble(" + paramVarName + ")";
            case "java.math.BigDecimal" -> "new java.math.BigDecimal(" + paramVarName + ")";
            case "java.lang.Boolean", "boolean" -> "Boolean.parseBoolean(" + paramVarName + ")";
            case "java.time.LocalDate" -> "java.time.LocalDate.parse(" + paramVarName + ")";
            case "java.time.LocalDateTime" -> "java.time.LocalDateTime.parse(" + paramVarName + ")";
            case "java.time.LocalTime" -> "java.time.LocalTime.parse(" + paramVarName + ")";
            default -> paramVarName;
        };
    }

    /**
     * 获取 QueryDSL 路径方法名
     */
    public String getQueryMethod() {
        return switch (queryType) {
            case EQ -> "eq";
            case LIKE, LEFT_LIKE, RIGHT_LIKE -> "like";
            case GT -> "gt";
            case GTE -> "goe";
            case LT -> "lt";
            case LTE -> "loe";
            case IN -> "in";
            case BETWEEN -> "between";
        };
    }

    /**
     * 获取 LIKE 包装后的值表达式
     */
    public String getLikeWrappedExpr(String valueExpr) {
        return switch (queryType) {
            case LIKE -> "\"%\" + " + valueExpr + " + \"%\"";
            case LEFT_LIKE -> valueExpr + " + \"%\"";
            case RIGHT_LIKE -> "\"%\" + " + valueExpr;
            default -> valueExpr;
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fieldName;
        private String column;
        private QueryType queryType = QueryType.EQ;
        private boolean ignoreNull = true;
        private boolean multi;
        private String rangeStart = "";
        private String rangeEnd = "";
        private boolean hasOrder;
        private boolean orderAsc = true;
        private int orderPriority;
        private boolean autoSort;
        private String typeQualifiedName;
        private String typeSimpleName;
        private String description = "";

        private Builder() {}

        public Builder fieldName(String fieldName) { this.fieldName = fieldName; return this; }

        public Builder column(String column) { this.column = column; return this; }

        public Builder queryType(QueryType queryType) { this.queryType = queryType; return this; }

        public Builder ignoreNull(boolean ignoreNull) { this.ignoreNull = ignoreNull; return this; }

        public Builder multi(boolean multi) { this.multi = multi; return this; }

        public Builder rangeStart(String rangeStart) { this.rangeStart = rangeStart; return this; }

        public Builder rangeEnd(String rangeEnd) { this.rangeEnd = rangeEnd; return this; }

        public Builder hasOrder(boolean hasOrder) { this.hasOrder = hasOrder; return this; }

        public Builder orderAsc(boolean orderAsc) { this.orderAsc = orderAsc; return this; }

        public Builder orderPriority(int orderPriority) { this.orderPriority = orderPriority; return this; }

        public Builder autoSort(boolean autoSort) { this.autoSort = autoSort; return this; }

        public Builder typeQualifiedName(String typeQualifiedName) { this.typeQualifiedName = typeQualifiedName; return this; }

        public Builder typeSimpleName(String typeSimpleName) { this.typeSimpleName = typeSimpleName; return this; }

        public Builder description(String description) { this.description = description != null ? description : ""; return this; }

        public QueryFieldMeta build() {
            if (column == null || column.isEmpty()) {
                column = fieldName;
            }
            return new QueryFieldMeta(this);
        }
    }
}