package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.DecimalFormat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class DecimalFormatSerializer extends StdSerializer<BigDecimal> implements ContextualSerializer {

    private final int scale;
    private final RoundingMode roundingMode;

    public DecimalFormatSerializer() {
        super(BigDecimal.class);
        this.scale = -1;
        this.roundingMode = null;
    }

    public DecimalFormatSerializer(int scale, RoundingMode roundingMode) {
        super(BigDecimal.class);
        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        DecimalFormat ann = property != null
                ? property.getAnnotation(DecimalFormat.class)
                : null;
        if (ann != null) {
            return new DecimalFormatSerializer(ann.scale(), RoundingMode.valueOf(ann.roundingMode()));
        }
        return this;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (scale >= 0 && roundingMode != null) {
            value = value.setScale(scale, roundingMode);
        }
        gen.writeString(value.toPlainString());
    }

    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
