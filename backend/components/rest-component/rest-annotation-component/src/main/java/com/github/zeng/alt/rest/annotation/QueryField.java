package com.github.zeng.alt.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {

    // 查询类型
    QueryType type() default QueryType.EQ;

    // 目标字段名（不写默认用字段名）
    String column() default "";

    // 是否允许 null 查询
    boolean ignoreNull() default false;

    // 是否支持多值 IN
    boolean multi() default false;
}