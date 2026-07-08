package com.github.zeng.alt.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryOrder {
    int order() default 0;
    boolean asc() default true;
    boolean autoSort() default false;
}