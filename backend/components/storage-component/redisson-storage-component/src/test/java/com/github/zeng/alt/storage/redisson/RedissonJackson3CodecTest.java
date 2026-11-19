package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.json.JsonConfiguration;
import io.netty.buffer.ByteBuf;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.redisson.codec.JsonJackson3Codec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.support.NullValue;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证缓存 JSON 的类型、日期与空值契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RedissonJackson3CodecTest {
    @Test
    void roundTripsTypedValuesAndNullSentinelsWithoutChangingTheApplicationMapper() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JsonConfiguration.class, JacksonAutoConfiguration.class))
                .run(context -> {
                    final JsonMapper applicationMapper = context.getBean(JsonMapper.class);
                    final String applicationJson = applicationMapper.writeValueAsString(LocalDate.of(2026, 11, 19));
                    final JsonJackson3Codec codec = new RedissonStorageAutoConfiguration(new RedissonProperties())
                            .jsonJacksonCodec(context.getBeanProvider(JacksonModule.class),
                                    context.getBeanProvider(JsonMapperBuilderCustomizer.class));
                    final CachedValue original = new CachedValue();
                    original.name = "cached";
                    original.date = LocalDate.of(2026, 11, 19);
                    original.timestamp = LocalDateTime.of(2026, 11, 19, 10, 20, 30);
                    original.absent = (NullValue) NullValue.INSTANCE;
                    final ByteBuf encoded = codec.getValueEncoder().encode(original);
                    try {
                        assertThat(codec.getValueDecoder().decode(encoded, null))
                                .usingRecursiveComparison().isEqualTo(original);
                    } finally {
                        encoded.release();
                    }
                    final ByteBuf absent = codec.getValueEncoder().encode(NullValue.INSTANCE);
                    try {
                        assertThat(codec.getValueDecoder().decode(absent, null)).isSameAs(NullValue.INSTANCE);
                    } finally {
                        absent.release();
                    }
                    assertThat(applicationMapper.writeValueAsString(original.date)).isEqualTo(applicationJson);
                });
    }

    static class CachedValue {
        private String name;
        private LocalDate date;
        private LocalDateTime timestamp;
        private NullValue absent;
    }
}
