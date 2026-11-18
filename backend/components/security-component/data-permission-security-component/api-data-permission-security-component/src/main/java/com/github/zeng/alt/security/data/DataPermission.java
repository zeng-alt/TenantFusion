package com.github.zeng.alt.security.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在方法调用期间启用目标实体的数据权限 SQL 过滤。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DataPermission {
    Class<?> resource();

    String permission();
}
