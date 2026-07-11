package com.github.zeng.alt.json.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.zeng.alt.json.serialize.DictFormatSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = DictFormatSerializer.class)
public @interface DictFormat {

    String dictType() default "";

    Class<? extends Enum<?>> enumClass() default NoDictEnum.class;

    enum NoDictEnum {}
}
