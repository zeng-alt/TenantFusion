package com.github.zeng.alt.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为 ConfigBean 的非 final 实例字段绑定标量或 JSON 配置。
 * 缺失时才使用 defaultValue，格式错误会阻止实例创建。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {
    String NO_DEFAULT = "__CONFIG_NO_DEFAULT_VALUE__";

    String value();

    /** 空分组继承所在 ConfigBean 的分组。 */
    String group() default "";

    String defaultValue() default NO_DEFAULT;
}
