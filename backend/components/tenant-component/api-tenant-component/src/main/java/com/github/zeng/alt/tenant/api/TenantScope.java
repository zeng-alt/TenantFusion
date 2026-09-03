package com.github.zeng.alt.tenant.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在实体上，覆盖该实体参与的隔离档位。
 * <p>
 * <b>只能表达行级与表级</b>：模式级和库级作用在连接层，粒度是整个 Session，
 * 不可能让同一个事务内的实体 A 落在 schema X、实体 B 落在 schema Y，因此那两档只能按租户配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantScope {

    /**
     * 是否参与行级隔离。实体需同时具备 {@code @TenantId} 字段（继承 {@code TenantBaseEntity} 即可）。
     *
     * @return 默认 true
     */
    boolean row() default true;

    /**
     * 是否参与表级隔离，即表名追加租户后缀。
     *
     * @return 默认 false
     */
    boolean table() default false;
}
