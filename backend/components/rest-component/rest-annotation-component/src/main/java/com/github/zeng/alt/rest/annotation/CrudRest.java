package com.github.zeng.alt.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Repository 接口自动生成 CRUD REST 接口
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CrudRest {

    /**
     * 路由路径前缀，例如 "/user"
     */
    String path();

    /**
     * 是否启用分页查询
     */
    boolean pageable() default true;

    /**
     * 是否生成 create 接口
     */
    boolean create() default true;

    /**
     * 是否生成 update 接口
     */
    boolean update() default true;

    /**
     * 是否生成 delete 接口
     */
    boolean delete() default true;

    /**
     * 是否生成 detail 接口
     */
    boolean detail() default true;

    /**
     * 是否生成 list 接口
     */
    boolean list() default true;
}
