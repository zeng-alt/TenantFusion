package com.github.zeng.alt.json.jackson3;

import com.github.zeng.alt.json.serialize.BigNumberConverter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 3 的业务注解、数值精度与日期格式。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class BusinessJacksonModule extends SimpleModule {
    public BusinessJacksonModule() {
        super("alt-business-jackson3");
        final var integers = new ConvertedValueSerializer(value -> BigNumberConverter.isSafeInteger((Number) value)
                ? value : value.toString());
        addSerializer(Long.class, integers);
        addSerializer(Long.TYPE, integers);
        addSerializer(BigInteger.class, integers);
        addSerializer(BigDecimal.class, new ConvertedValueSerializer(Object::toString));
        final DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss");
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTime));
        addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime));
        addSerializer(LocalDate.class, new LocalDateSerializer(date));
        addDeserializer(LocalDate.class, new LocalDateDeserializer(date));
        addSerializer(LocalTime.class, new LocalTimeSerializer(time));
        addDeserializer(LocalTime.class, new LocalTimeDeserializer(time));
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.appendAnnotationIntrospector(new BusinessAnnotationIntrospector());
    }
}
