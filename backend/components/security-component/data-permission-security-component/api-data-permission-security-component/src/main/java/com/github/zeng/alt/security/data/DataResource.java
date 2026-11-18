package com.github.zeng.alt.security.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识参与数据权限过滤的 JPA 资源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DataResource {
    String value();
}
