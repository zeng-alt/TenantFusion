package com.github.zeng.alt.json;

import com.github.zeng.alt.api.base.BaseEnum;
import com.github.zeng.alt.json.annotation.DecimalFormat;
import com.github.zeng.alt.json.annotation.Decrypt;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.annotation.Encrypt;
import com.github.zeng.alt.json.annotation.NameDesensitize;
import com.github.zeng.alt.json.annotation.NullToDefault;
import com.github.zeng.alt.json.annotation.NullToEmpty;
import com.github.zeng.alt.json.annotation.NullToZero;
import com.github.zeng.alt.json.annotation.Sensitive;
import com.github.zeng.alt.json.annotation.SensitiveType;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson 3 的业务注解边界契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JsonAnnotationContractTest {
    @ParameterizedTest
    @MethodSource("masks")
    void masksFieldsWithTheExistingContract(Object value, String expected) {
        runner().run(context -> {
            final var modern = context.getBean(JsonMapper.class);
            assertThat(modern.readTree(modern.writeValueAsString(value)).get("value").asString()).isEqualTo(expected);
        });
    }

    static Stream<Arguments> masks() {
        return Stream.of(
                Arguments.of(new Phone("123"), "1**"), Arguments.of(new Phone(""), ""),
                Arguments.of(new Name("张三"), "张*"), Arguments.of(new OldName("张三"), "张*"),
                Arguments.of(new Email("test@example.com"), "t***@example.com"),
                Arguments.of(new Email("invalid"), "invalid"),
                Arguments.of(new IdCard("110101199001011234"), "110***********1234"),
                Arguments.of(new IdCard("123"), "123"),
                Arguments.of(new BankCard("6222021234561234"), "6222********1234"),
                Arguments.of(new BankCard("123"), "123"),
                Arguments.of(new Address("北京市海淀区中关村"), "北京市海淀区***"),
                Arguments.of(new Address("北京"), "北京"),
                Arguments.of(new Password("secret"), "******")
        );
    }

    @Test
    void usesPlainValuesWhenOptionalServicesAreAbsentAndKeepsNonNullValues() {
        DictServiceHolder.setService(null);
        EncryptServiceHolder.setService(null);
        runner().run(context -> {
            final var modern = context.getBean(JsonMapper.class);
            final var payload = new NonNullValues("name", "remark", 12, "plain", "raw", new BigDecimal("1.234"));
            final String json = modern.writeValueAsString(payload);
            final var values = modern.readTree(json);
            assertThat(values.get("name").asString()).isEqualTo("name");
            assertThat(values.get("remark").asString()).isEqualTo("remark");
            assertThat(values.get("count").asInt()).isEqualTo(12);
            assertThat(values.get("amount").asString()).isEqualTo("1.234");
            assertThat(values.get("secret").asString()).isEqualTo("plain");
            assertThat(modern.readValue("{\"value\":\"plain\"}", Secret.class).value()).isEqualTo("plain");
            assertThat(modern.readValue("{\"value\":\"\"}", Secret.class).value()).isEmpty();
            assertThat(modern.readValue("{\"value\":null}", Secret.class).value()).isNull();
            final var dictionaries = modern.readTree(modern.writeValueAsString(new Dictionaries("1", "missing", "A")));
            assertThat(dictionaries.get("known").asString()).isEqualTo("启用");
            assertThat(dictionaries.get("unknown").asString()).isEqualTo("missing");
            assertThat(dictionaries.get("regular").asString()).isEqualTo("A");
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JsonConfiguration.class, JacksonAutoConfiguration.class
        ));
    }

    private record Phone(
        @Sensitive(type = SensitiveType.PHONE) String value
    ) {
    }
    private record Name(
        @Sensitive(type = SensitiveType.NAME) String value
    ) {
    }
    private record OldName(
        @NameDesensitize String value
    ) {
    }
    private record Email(
        @Sensitive(type = SensitiveType.EMAIL) String value
    ) {
    }
    private record IdCard(
        @Sensitive(type = SensitiveType.ID_CARD) String value
    ) {
    }
    private record BankCard(
        @Sensitive(type = SensitiveType.BANK_CARD) String value
    ) {
    }
    private record Address(
        @Sensitive(type = SensitiveType.ADDRESS) String value
    ) {
    }
    private record Password(
        @Sensitive(type = SensitiveType.PASSWORD) String value
    ) {
    }
    private record Secret(
        @Decrypt String value
    ) {
    }

    private record NonNullValues(
        @NullToEmpty String name,
        @NullToDefault("fallback") String remark,
        @NullToZero Integer count,
        @Encrypt String secret,
        @DictFormat(dictType = "missing") String code,
        @DecimalFormat(scale = -1) BigDecimal amount
    ) {
    }

    private record Dictionaries(
        @DictFormat(enumClass = Status.class) String known,
        @DictFormat(enumClass = Status.class) String unknown,
        @DictFormat(enumClass = Regular.class) String regular
    ) {
    }

    enum Status implements BaseEnum {
        ENABLED;
        @Override
        public Integer getCode() {
            return 1;
        }
        @Override
        public String getLabel() {
            return "启用";
        }
    }

    enum Regular { A }
}
