package com.github.zeng.alt.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将 Spring Bean 方法绑定到一个明确配置的消费端点。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSubscription {
    /** 应用内唯一的监听端点 ID，支持 Spring 属性占位符。 */
    String id();

    /** 对应 alt.event.bindings 下的逻辑绑定名，支持 Spring 属性占位符。 */
    String binding();
}
