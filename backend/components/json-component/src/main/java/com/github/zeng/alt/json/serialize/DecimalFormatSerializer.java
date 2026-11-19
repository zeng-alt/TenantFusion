package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.DecimalFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class DecimalFormatSerializer extends StdSerializer<BigDecimal> {

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
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        DecimalFormat ann = property != null
                ? property.getAnnotation(DecimalFormat.class)
                : null;
        if (ann != null) {
            return new DecimalFormatSerializer(ann.scale(), RoundingMode.valueOf(ann.roundingMode()));
        }
        return this;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext provider) {
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
    public void serializeWithType(BigDecimal value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
