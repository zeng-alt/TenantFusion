package com.github.zeng.alt.tenant.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在实体上，声明该表为全租户共享，不参与任何隔离。
 * <p>
 * {@code main_tenant} 自身必须标注本注解——查租户元数据前先要知道租户，否则形成死循环。
 * 优先级高于 {@link TenantScope} 与全局配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantIgnore {
}
