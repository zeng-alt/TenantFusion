package com.github.zeng.alt.security.abac;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级 ABAC 前置授权注解。标注后由 method-abac 拦截器在方法执行前完成
 * 认证/租户/RBAC 预检与属性约束，拒绝抛 {@code AccessDeniedException}。
 * <p>上下文包含 principal/resource/env/authentication，方法参数通过 #参数名/#p0/#a0 读取。
 * 后置规则在方法返回后、事务提交前执行；没有活动事务时拒绝执行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AbacAuthorize {

    /** 权限码，对应 main_permission.code；宿主用它做 RBAC 预检并加载策略。 */
    String permission();

    /** 无适用策略时是否拒绝；默认 false（无策略放行，与 AbacTemplate.get 一致）。 */
    boolean requirePolicy() default false;

    /** 资源解析器列表，宿主实现 {@link AbacResourceResolver} 并注册为 Spring bean；按声明顺序合并资源属性。缺省无资源属性。 */
    Class<? extends AbacResourceResolver>[] resources() default {};
}