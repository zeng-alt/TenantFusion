package com.github.zeng.alt.json.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.zeng.alt.json.serialize.DictFormatSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON 序列化时对字段进行字典翻译转换。
 * <p>
 * <b>两种翻译模式（二选一）：</b><br>
 * 1. <b>数据库字典</b>：指定 {@link #dictType()}，需要注册 {@link com.github.zeng.alt.json.spi.IDictTranslateService} 实现<br>
 * 2. <b>Java 枚举</b>：指定 {@link #enumClass()}，枚举需实现 {@link com.github.zeng.alt.json.spi.IDictEnum}
 * 或 {@link com.github.zeng.alt.api.base.BaseEnum} 接口
 * <p>
 * 当同时指定两者时，{@link #enumClass()} 优先。
 *
 * <pre>{@code
 * // 数据库字典模式
 * @DictFormat(dictType = "user_status")
 * private String status;
 *
 * // Java 枚举模式
 * @DictFormat(enumClass = StatusEnum.class)
 * private String status;
 * }</pre>
 *
 * @author zengJiaJun
 * @since 2026年07月11日
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = DictFormatSerializer.class)
public @interface DictFormat {

    /** 数据库字典类型编码，与 {@link com.github.zeng.alt.json.spi.IDictTranslateService#translate(String, String)} 的 dictType 参数对应。 */
    String dictType() default "";

    /** Java 枚举类，枚举常量需实现 {@link com.github.zeng.alt.json.spi.IDictEnum} 或 {@link com.github.zeng.alt.api.base.BaseEnum}。 */
    Class<? extends Enum<?>> enumClass() default NoDictEnum.class;

    enum NoDictEnum {}
}
