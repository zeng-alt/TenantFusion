package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.api.base.BaseEnum;
import com.github.zeng.alt.json.internal.DictValueHelper;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.IDictEnum;
import com.github.zeng.alt.json.spi.IDictTranslateService;

import java.util.Map;
import java.util.Objects;

/**
 * 字典翻译序列化器，在 {@code createContextual} 阶段预构建 code→label 映射，
 * 运行时零反射，兼容 GraalVM Native Image。
 * <p>
 * 两种翻译来源：
 * <ul>
 *   <li><b>Java 枚举</b>：枚举常量实现 {@link IDictEnum} 或 {@link BaseEnum}，通过 {@link IDictEnum#getCode()} /
 *       {@link BaseEnum#getCode()} 匹配字段值，输出 {@link IDictEnum#getLabel()}</li>
 *   <li><b>数据库字典</b>：通过 {@link IDictTranslateService#translate(String, String)} 实时查询</li>
 * </ul>
 */
@JacksonStdImpl
public class DictFormatSerializer extends StdSerializer<Object> {

    private final String dictType;
    private final Map<String, String> enumLabelMap;

    public DictFormatSerializer() {
        super(Object.class);
        this.dictType = null;
        this.enumLabelMap = null;
    }

    public DictFormatSerializer(String dictType, Map<String, String> enumLabelMap) {
        super(Object.class);
        this.dictType = dictType;
        this.enumLabelMap = enumLabelMap;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        DictFormat ann = property != null
                ? property.getAnnotation(DictFormat.class)
                : null;
        if (ann == null) {
            return this;
        }
        final Map<String, String> map = DictValueHelper.enumLabels(ann.enumClass());
        return new DictFormatSerializer(ann.dictType(), map.isEmpty() ? null : map);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String code = value.toString();

        if (enumLabelMap != null) {
            String label = enumLabelMap.get(code);
            if (label != null) {
                gen.writeString(label);
                return;
            }
            gen.writeString(code);
            return;
        }

        if (dictType != null && !dictType.isEmpty()) {
            IDictTranslateService service = DictServiceHolder.getService();
            if (service != null) {
                String label = service.translate(dictType, code);
                gen.writeString(Objects.requireNonNullElse(label, code));
                return;
            }
        }

        gen.writeString(code);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
