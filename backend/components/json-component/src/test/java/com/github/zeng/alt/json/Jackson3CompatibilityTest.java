package com.github.zeng.alt.json;

import com.github.zeng.alt.json.annotation.DecimalFormat;
import com.github.zeng.alt.json.annotation.Decrypt;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.annotation.Encrypt;
import com.github.zeng.alt.json.annotation.NullToDefault;
import com.github.zeng.alt.json.annotation.NullToEmpty;
import com.github.zeng.alt.json.annotation.NullToZero;
import com.github.zeng.alt.json.annotation.Sensitive;
import com.github.zeng.alt.json.annotation.SensitiveType;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IDictEnum;
import com.github.zeng.alt.json.spi.IDictTranslateService;
import com.github.zeng.alt.json.spi.IEncryptService;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson 3 与现有业务 JSON 契约的兼容测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class Jackson3CompatibilityTest {
    @AfterEach
    void clearServices() {
        DictServiceHolder.setService(null);
        EncryptServiceHolder.setService(null);
    }

    @Test
    void supportsTheExistingAnnotationsDatesAndBusinessServices() {
        runner().withUserConfiguration(Services.class).run(context -> {
            final JsonMapper mapper = context.getBean(JsonMapper.class);
            final var json = mapper.readTree(mapper.writeValueAsString(new Payload()));
            assertThat(json.get("phone").asString()).isEqualTo("138####5678");
            assertThat(json.get("status").asString()).isEqualTo("启用");
            assertThat(json.get("category").asString()).isEqualTo("db:category:1");
            assertThat(json.get("secret").asString()).isEqualTo("enc:value");
            assertThat(json.get("amount").asString()).isEqualTo("12.35");
            assertThat(json.get("precise").asString()).isEqualTo("12.346");
            assertThat(json.get("empty").asString()).isEmpty();
            assertThat(json.get("fallback").asString()).isEqualTo("N/A");
            assertThat(json.get("zero").intValue()).isZero();
            assertThat(json.get("id").isString()).isTrue();
            assertThat(json.get("date").asString()).isEqualTo("2026-11-16");
            assertThat(json.get("time").asString()).isEqualTo("10:20:30");
            assertThat(json.get("timestamp").asString()).isEqualTo("2026-11-16 10:20:30");
            assertThat(mapper.readValue("\"2026-11-16 10:20:30\"", LocalDateTime.class))
                    .isEqualTo(LocalDateTime.of(2026, 11, 16, 10, 20, 30));
            assertThat(mapper.readValue("{\"secret\":\"enc:value\"}", Secret.class).secret).isEqualTo("value");
        });
    }

    @Test
    void retainsTheJackson2MapperAndItsAnnotationContract() {
        runner().withUserConfiguration(Services.class).run(context -> {
            final var mapper = context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class);
            final var json = mapper.readTree(mapper.writeValueAsString(new Payload()));
            assertThat(json.get("phone").asText()).isEqualTo("138####5678");
            assertThat(json.get("status").asText()).isEqualTo("启用");
            assertThat(json.get("secret").asText()).isEqualTo("enc:value");
            assertThat(json.get("empty").asText()).isEmpty();
            assertThat(json.get("fallback").asText()).isEqualTo("N/A");
            assertThat(json.get("zero").intValue()).isZero();
        });
    }

    @Test
    void roundTripsVavrValuesUsingTheNativeJackson3Mapper() {
        runner().run(context -> {
            final JsonMapper mapper = context.getBean(JsonMapper.class);
            final var value = new Values(Option.some("kept"), Either.right(3),
                    io.vavr.collection.List.of(1, 2), io.vavr.collection.HashMap.of("a", 1));
            final String json = mapper.writeValueAsString(value);
            assertThat(mapper.readTree(json).get("optional").asString()).isEqualTo("kept");
            assertThat(mapper.readValue(json, Values.class)).isEqualTo(value);
            assertThat(mapper.readValue("null", new TypeReference<Option<String>>() {})).isEqualTo(Option.none());
            assertThat(mapper.readValue("[1,2]", new TypeReference<io.vavr.collection.List<Integer>>() {}))
                    .isEqualTo(io.vavr.collection.List.of(1, 2));
        });
    }

    @Test
    void writesAllOutOfRangeBigIntegersAsStrings() {
        runner().run(context -> {
            final BigInteger value = BigInteger.ONE.shiftLeft(64);
            final JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.readTree(mapper.writeValueAsString(value)).isString()).isTrue();
            final var legacy = context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class);
            assertThat(legacy.readTree(legacy.writeValueAsString(value)).isTextual()).isTrue();
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JsonConfiguration.class, JacksonAutoConfiguration.class, Jackson2AutoConfiguration.class
        ));
    }

    @Configuration(proxyBeanMethods = false)
    static class Services {
        @Bean
        IDictTranslateService dictionary() {
            return (type, code) -> "db:" + type + ":" + code;
        }

        @Bean
        IEncryptService encryption() {
            return new IEncryptService() {
                @Override
                public String encrypt(String value) {
                    return "enc:" + value;
                }

                @Override
                public String decrypt(String value) {
                    return value.substring(4);
                }
            };
        }
    }

    enum Status implements IDictEnum {
        ENABLED;

        @Override
        public String getLabel() {
            return "启用";
        }
    }

    static class Payload {
        @Sensitive(type = SensitiveType.PHONE, placeholder = "#")
        public String phone = "13812345678";
        @DictFormat(enumClass = Status.class)
        public String status = "ENABLED";
        @DictFormat(dictType = "category")
        public String category = "1";
        @Encrypt
        public String secret = "value";
        @DecimalFormat(scale = 2)
        public BigDecimal amount = new BigDecimal("12.3456");
        @DecimalFormat(scale = 3)
        public BigDecimal precise = new BigDecimal("12.3456");
        @NullToEmpty
        public String empty;
        @NullToDefault("N/A")
        public String fallback;
        @NullToZero
        public Integer zero;
        public long id = 9007199254740993L;
        public LocalDate date = LocalDate.of(2026, 11, 16);
        public LocalTime time = LocalTime.of(10, 20, 30);
        public LocalDateTime timestamp = LocalDateTime.of(date, time);
    }

    static class Secret {
        @Decrypt
        public String secret;
    }

    private record Values(
        Option<String> optional,
        Either<String, Integer> either,
        io.vavr.collection.List<Integer> values,
        io.vavr.collection.Map<String, Integer> mapping
    ) {
    }
}
