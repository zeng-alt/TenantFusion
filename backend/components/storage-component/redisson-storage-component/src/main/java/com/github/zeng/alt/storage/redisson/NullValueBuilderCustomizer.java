package com.github.zeng.alt.storage.redisson;


import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.cache.support.NullValue;
import tools.jackson.databind.json.JsonMapper;

public final class NullValueBuilderCustomizer implements JsonMapperBuilderCustomizer {

    public static final NullValueBuilderCustomizer INSTANCE = new NullValueBuilderCustomizer();

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.addMixIn(NullValue.class, UseTypeInfo.class);
        builder.addModule(new tools.jackson.databind.module.SimpleModule("alt-cache-null")
                .addDeserializer(NullValue.class, NullValueDeserializer.INSTANCE));
    }

    public static class NullValueDeserializer extends StdDeserializer<NullValue> {

        public static final NullValueDeserializer INSTANCE = new NullValueDeserializer();

        protected NullValueDeserializer() {
            super(NullValue.class);
        }

        @Override
        public NullValue deserialize(JsonParser p, DeserializationContext ctx) {
            return (NullValue) NullValue.INSTANCE;
        }
    }

}

// Mix-in annotation to apply only to record classes
