package com.github.zeng.alt.config.annotation;

import com.github.zeng.alt.config.ConfigTemplate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 监听指定配置，方法接受零个参数或一个 ConfigChangeEvent。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigListener {
    String[] keys();

    String group() default ConfigTemplate.DEFAULT_GROUP;
}
