package com.github.zeng.alt.domain.key;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author zengJiaJun
 * @since 2025年12月15日 13:45
 * @version 1.0
 */
@IdGeneratorType(SnowflakeIdGenerator.class)
@Retention(RUNTIME)
@Target({METHOD,FIELD})
public @interface SnowflakeId {
}
