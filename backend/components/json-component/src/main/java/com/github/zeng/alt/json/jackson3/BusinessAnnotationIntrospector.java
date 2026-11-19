package com.github.zeng.alt.json.jackson3;

import com.github.zeng.alt.json.annotation.DecimalFormat;
import com.github.zeng.alt.json.annotation.Decrypt;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.annotation.Encrypt;
import com.github.zeng.alt.json.annotation.NameDesensitize;
import com.github.zeng.alt.json.annotation.NullToDefault;
import com.github.zeng.alt.json.annotation.NullToEmpty;
import com.github.zeng.alt.json.annotation.NullToZero;
import com.github.zeng.alt.json.annotation.PhoneDesensitize;
import com.github.zeng.alt.json.annotation.Sensitive;
import com.github.zeng.alt.json.annotation.SensitiveType;
import com.github.zeng.alt.json.internal.DictValueHelper;
import com.github.zeng.alt.json.internal.SensitiveValueHelper;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * 为 Jackson 3 补充业务注解的空值处理与转换行为。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class BusinessAnnotationIntrospector extends NopAnnotationIntrospector {
    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated member) {
        final Sensitive sensitive = member.getAnnotation(Sensitive.class);
        if (sensitive != null) {
            return mask(sensitive.type(), sensitive.placeholder());
        }
        if (member.hasAnnotation(PhoneDesensitize.class)) {
            return mask(SensitiveType.PHONE, "*");
        }
        if (member.hasAnnotation(NameDesensitize.class)) {
            return mask(SensitiveType.NAME, "*");
        }
        final DictFormat dict = member.getAnnotation(DictFormat.class);
        if (dict != null) {
            return dictionary(dict);
        }
        if (member.hasAnnotation(Encrypt.class)) {
            return new ConvertedValueSerializer(value -> Option.of(EncryptServiceHolder.getService())
                    .filter(service -> !value.toString().isEmpty())
                    .map(service -> service.encrypt(value.toString())).getOrElse(value.toString()));
        }
        final DecimalFormat decimal = member.getAnnotation(DecimalFormat.class);
        if (decimal != null) {
            final RoundingMode rounding = RoundingMode.valueOf(decimal.roundingMode());
            return new ConvertedValueSerializer(value -> decimal.scale() < 0 ? ((BigDecimal) value).toPlainString()
                    : ((BigDecimal) value).setScale(decimal.scale(), rounding).toPlainString());
        }
        return findNullSerializer(config, member);
    }

    @Override
    public Object findNullSerializer(MapperConfig<?> config, Annotated member) {
        if (member.hasAnnotation(NullToEmpty.class)) {
            return new ConvertedValueSerializer(value -> value == null ? "" : value.toString());
        }
        final NullToDefault defaults = member.getAnnotation(NullToDefault.class);
        if (defaults != null) {
            return new ConvertedValueSerializer(value -> value == null ? defaults.value() : value.toString());
        }
        if (member.hasAnnotation(NullToZero.class)) {
            return new ConvertedValueSerializer(value -> value == null ? 0 : value);
        }
        return null; // Jackson introspector SPI uses null to delegate to the next introspector.
    }

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated member) {
        return member.hasAnnotation(Decrypt.class) ? new DecryptValueDeserializer() : null;
    }

    private ConvertedValueSerializer mask(SensitiveType type, String placeholder) {
        return new ConvertedValueSerializer(value -> SensitiveValueHelper.mask(value.toString(), type, placeholder));
    }

    private ConvertedValueSerializer dictionary(DictFormat annotation) {
        final Map<String, String> labels = DictValueHelper.enumLabels(annotation.enumClass());
        return new ConvertedValueSerializer(value -> {
            final String code = value.toString();
            if (!labels.isEmpty()) {
                return labels.getOrDefault(code, code);
            }
            return Option.of(DictServiceHolder.getService()).filter(service -> !annotation.dictType().isEmpty())
                    .flatMap(service -> Option.of(service.translate(annotation.dictType(), code))).getOrElse(code);
        });
    }
}
