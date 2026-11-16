package com.github.zeng.alt.config.annotation;

import com.github.zeng.alt.config.ConfigTemplate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;

/**
 * 配置变化后销毁旧实例，下次通过代理调用时重新创建 Bean。
 * keys 声明工厂方法使用的配置；字段上的 ConfigValue 会自动贡献依赖。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Scope(value = "businessConfig", proxyMode = ScopedProxyMode.TARGET_CLASS)
public @interface ConfigBean {
    String[] keys() default {};

    String group() default ConfigTemplate.DEFAULT_GROUP;

    @AliasFor(annotation = Scope.class, attribute = "proxyMode")
    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}
