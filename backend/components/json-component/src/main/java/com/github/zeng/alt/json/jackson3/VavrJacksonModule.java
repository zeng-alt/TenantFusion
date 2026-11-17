package com.github.zeng.alt.json.jackson3;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.Traversable;
import io.vavr.control.Either;
import io.vavr.control.Option;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.Serializers;

/**
 * Vavr 的 Jackson 3 适配，保留 Option、Either 和集合的现有 JSON 形状。
 * 与 Jackson 2 的 vavr-jackson 模块使用独立命名空间。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class VavrJacksonModule extends SimpleModule {
    public VavrJacksonModule() {
        super("alt-vavr-jackson3");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addSerializers(new Serializers.Base() {
            @Override
            public ValueSerializer<?> findSerializer(
                    SerializationConfig config,
                    JavaType type,
                    BeanDescription.Supplier description,
                    JsonFormat.Value format
            ) {
                return supports(type) ? new VavrValueSerializer() : null;
            }
        });
        context.addDeserializers(new Deserializers.Base() {
            @Override
            public boolean hasDeserializerFor(DeserializationConfig config, Class<?> raw) {
                return supports(config.constructType(raw));
            }

            @Override
            public ValueDeserializer<?> findBeanDeserializer(
                    JavaType type,
                    DeserializationConfig config,
                    BeanDescription.Supplier description
            ) {
                return supports(type) ? new VavrValueDeserializer(type) : null;
            }
        });
    }

    private static boolean supports(JavaType type) {
        final Class<?> raw = type.getRawClass();
        return Option.class.isAssignableFrom(raw) || Either.class.isAssignableFrom(raw)
                || Lazy.class.isAssignableFrom(raw) || raw == Traversable.class
                || io.vavr.collection.Seq.class.isAssignableFrom(raw)
                || io.vavr.collection.Set.class.isAssignableFrom(raw)
                || io.vavr.collection.Map.class.isAssignableFrom(raw)
                || io.vavr.collection.PriorityQueue.class.isAssignableFrom(raw)
                || Tuple.class.isAssignableFrom(raw);
    }
}
