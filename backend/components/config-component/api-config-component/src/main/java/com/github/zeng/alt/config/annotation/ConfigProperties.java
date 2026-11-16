package com.github.zeng.alt.config.annotation;

import com.github.zeng.alt.config.ConfigTemplate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 按 Spring Boot 配置属性约定绑定业务配置，自动注册并在配置变化时刷新。
 * 默认值直接声明在字段上；配置键使用完整前缀，例如 payment.enabled。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
@Scope(value = "businessConfig", proxyMode = ScopedProxyMode.TARGET_CLASS)
public @interface ConfigProperties {
    String prefix();

    String group() default ConfigTemplate.DEFAULT_GROUP;

    @AliasFor(annotation = Scope.class, attribute = "proxyMode")
    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}
