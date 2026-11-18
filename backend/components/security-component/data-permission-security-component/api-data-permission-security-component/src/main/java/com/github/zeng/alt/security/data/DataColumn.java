package com.github.zeng.alt.security.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明实体属性在数据范围中的用途。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataColumn {
    DataColumnType value();
}
