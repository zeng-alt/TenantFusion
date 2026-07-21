package com.github.zeng.alt.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Repository 接口自动生成 CRUD REST 接口
 *
 * @author zengJiaJun
 * @since 2026年05月28日
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
     * 是否生成 update 接口（全量更新 PUT）
     */
    boolean update() default true;

    /**
     * 是否生成 patch 接口（部分更新 PATCH，只更新非 null 字段）
     */
    boolean patch() default true;

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

    /**
     * 是否生成 listAll 接口（条件查询所有，不分页，POST）
     */
    boolean listAll() default false;

    /**
     * 是否生成 jpa-search-helper 接口
     */
    boolean search() default false;

    /**
     * 是否生成查询 tree 接口
     */
    boolean tree() default false;

    /**
     * 是否生成 sort 接口
     */
    boolean sort() default false;


    /**
     * 是否生成 treeSort 接口
     */
    boolean treeSort() default false;

    /**
     * 查询条件 DTO
     * 如果不填 → 使用 entity
     */
    Class<?> queryType() default Void.class;

    /**
     * 新增接口请求体 DTO，如果不填 → 使用 entity
     */
    Class<?> createType() default Void.class;

    /**
     * 更新接口请求体 DTO，如果不填 → 使用 entity
     */
    Class<?> updateType() default Void.class;

    /**
     * 部分更新接口请求体 DTO，如果不填 → 优先使用 updateType，其次使用 entity
     */
    Class<?> patchType() default Void.class;

    /**
     * 详情接口返回体 DTO，如果不填 → 使用 entity
     */
    Class<?> detailType() default Void.class;

    /**
     * 列表和分页接口返回体 DTO，如果不填 → 使用 entity
     */
    Class<?> listType() default Void.class;

    /**
     * 树接口返回体 DTO，如果不填 → 使用 entity
     */
    Class<?> treeType() default Void.class;

    /**
     * jpa-search-helper接口返回体 DTO，如果不填 → 使用 entity
     */
    Class<?> searchType() default Void.class;
}
