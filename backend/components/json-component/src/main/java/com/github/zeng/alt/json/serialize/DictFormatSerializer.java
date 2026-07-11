package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.api.base.BaseEnum;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.IDictEnum;
import com.github.zeng.alt.json.spi.IDictTranslateService;

import java.io.IOException;
import java.util.HashMap;
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
public class DictFormatSerializer extends StdSerializer<Object> implements ContextualSerializer {

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
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        DictFormat ann = property != null
                ? property.getAnnotation(DictFormat.class)
                : null;
        if (ann == null) {
            return this;
        }
        Map<String, String> map = buildEnumMap(ann.enumClass());
        return new DictFormatSerializer(ann.dictType(), map);
    }

    /**
     * 预构建枚举 code→label 映射。支持三种枚举形式（按优先级）：
     * <ol>
     *   <li>{@link IDictEnum}：使用 {@link IDictEnum#getCode()}（为 null 时回退到 {@link Enum#name()}）</li>
     *   <li>{@link BaseEnum}：使用 {@link BaseEnum#getCode()}（转为字符串）</li>
     *   <li>普通枚举：使用 {@link Enum#name()} 作为 code 和 label</li>
     * </ol>
     */
    private static Map<String, String> buildEnumMap(Class<? extends Enum<?>> enumClass) {
        if (enumClass == DictFormat.NoDictEnum.class) {
            return null;
        }
        Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        Map<String, String> map = new HashMap<>(constants.length);
        for (Enum<?> constant : constants) {
            String code;
            String label;
            if (constant instanceof IDictEnum de) {
                code = de.getCode();
                if (code == null) {
                    code = constant.name();
                }
                label = de.getLabel();
            } else if (constant instanceof BaseEnum be) {
                code = String.valueOf(be.getCode());
                label = be.getLabel();
            } else {
                code = constant.name();
                label = constant.name();
            }
            map.put(code, label);
        }
        return Map.copyOf(map);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
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
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
