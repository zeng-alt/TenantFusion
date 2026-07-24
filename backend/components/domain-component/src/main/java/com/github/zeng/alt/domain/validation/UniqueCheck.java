package com.github.zeng.alt.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotEmpty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验指定字段在数据库中是否唯一。标注在类上。
 * <p>
 * 使用方式：
 * <pre>{@code
 * @UniqueCheck(field = "username")
 * public class UserCreateDTO {
 *     private String username;
 * }
 * }</pre>
 * 需要在 Spring 容器中注册 {@link IUniqueCheckRepository} 实现。
 * 模块已提供基于 EntityManager 的默认实现，会自动注入。
 *
 * @author zengJiaJun
 * @since 2026年07月23日
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueValidator.class)
public @interface UniqueCheck {
    @NotEmpty
    /** 要校验的实体类。默认 {@code void.class} 表示使用被校验对象本身的类型。 */
    Class<?> entity() default void.class;

    /** 要校验唯一性的字段名 */
    String field();

    /** 主键字段名（更新时排除自身） */
    String idField() default "id";

    String message() default "字段【{field}】数据已存在";

    Class<?>[] groups() default {};

    /**
     * 是否忽略大小写。
     * false：大小写敏感（默认）
     * true：大小写不敏感
     */
    boolean ignoreCase() default false;

    Class<? extends Payload>[] payload() default {};
}
