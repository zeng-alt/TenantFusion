package com.github.zeng.alt.json;

import com.github.zeng.alt.json.serialize.DecimalFormatSerializer;
import com.github.zeng.alt.json.serialize.DecryptDeserializer;
import com.github.zeng.alt.json.serialize.DictFormatSerializer;
import com.github.zeng.alt.json.serialize.EncryptSerializer;
import com.github.zeng.alt.json.serialize.NameDesensitizeSerializer;
import com.github.zeng.alt.json.serialize.NullToDefaultSerializer;
import com.github.zeng.alt.json.serialize.NullToEmptySerializer;
import com.github.zeng.alt.json.serialize.NullToZeroSerializer;
import com.github.zeng.alt.json.serialize.PhoneDesensitizeSerializer;
import com.github.zeng.alt.json.serialize.SensitiveSerializer;
import java.util.stream.Stream;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 注册组件资源以及 Jackson 2 通过业务注解实例化的序列化器。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class JsonRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("alt-json-defaults.properties");
        Stream.of(
                DecimalFormatSerializer.class,
                DecryptDeserializer.class,
                DictFormatSerializer.class,
                EncryptSerializer.class,
                NameDesensitizeSerializer.class,
                NullToDefaultSerializer.class,
                NullToEmptySerializer.class,
                NullToZeroSerializer.class,
                PhoneDesensitizeSerializer.class,
                SensitiveSerializer.class
        ).forEach(type -> hints.reflection().registerType(type, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
    }
}
