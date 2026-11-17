package com.github.zeng.alt.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.json.annotation.PhoneDesensitize;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import com.github.zeng.alt.json.serialize.SensitiveSerializer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 验证 Boot 4 默认使用 Jackson 3，并支持显式选择 Jackson 2。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JsonConfigurationTest {

    private static final AutoConfigurations JSON_CONFIGURATIONS = AutoConfigurations.of(
            JsonConfiguration.class, Jackson2AutoConfiguration.class, JacksonAutoConfiguration.class
    );

    @Test
    void registersNativeResourcesAndLegacySerializerConstructors() {
        final RuntimeHints hints = new RuntimeHints();
        new JsonRuntimeHints().registerHints(hints, getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource().forResource("alt-json-defaults.properties").test(hints)).isTrue();
        assertThat(hints.reflection().getTypeHint(SensitiveSerializer.class)).isNotNull();
    }

    @Test
    void registersBusinessModulesAlongsideJackson3AndSupportsDateRoundTrip() {
        new ApplicationContextRunner().withConfiguration(JSON_CONFIGURATIONS).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ObjectMapper.class);
            final ObjectMapper mapper = context.getBean(ObjectMapper.class);
            final String json = mapper.writeValueAsString(new Payload());
            assertPayload(mapper, json);
            assertThat(mapper.readValue("\"2026-11-12 10:20:30\"", LocalDateTime.class))
                    .isEqualTo(LocalDateTime.of(2026, 11, 12, 10, 20, 30));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "jackson2"})
    void servletHttpUsesJackson3ByDefaultAndCanSelectJackson2(String preferred) {
        new WebApplicationContextRunner()
                .withPropertyValues(preferred.equals("default") ? new String[0]
                        : new String[]{"spring.http.converters.preferred-json-mapper=" + preferred})
                .withConfiguration(JSON_CONFIGURATIONS)
                .withConfiguration(AutoConfigurations.of(
                        HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class
                ))
                .withUserConfiguration(EndpointConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var converter = context.getBean(RequestMappingHandlerAdapter.class).getMessageConverters()
                            .stream().filter(candidate -> candidate.canWrite(Payload.class, MediaType.APPLICATION_JSON))
                            .findFirst().orElseThrow();
                    assertThat(converter).isInstanceOf(preferred.equals("default")
                            ? JacksonJsonHttpMessageConverter.class : MappingJackson2HttpMessageConverter.class);
                    final String json = MockMvcBuilders.webAppContextSetup(context).build()
                            .perform(get("/json-contract")).andReturn().getResponse().getContentAsString();
                    assertPayload(context.getBean(ObjectMapper.class), json);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "jackson2"})
    void reactiveHttpUsesJackson3ByDefaultAndCanSelectJackson2(String preferred) {
        new ReactiveWebApplicationContextRunner()
                .withPropertyValues(preferred.equals("default") ? new String[0]
                        : new String[]{"spring.http.codecs.preferred-json-mapper=" + preferred})
                .withConfiguration(JSON_CONFIGURATIONS)
                .withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class, WebFluxAutoConfiguration.class))
                .withUserConfiguration(EndpointConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var writer = context.getBean(ServerCodecConfigurer.class).getWriters().stream()
                            .filter(candidate -> candidate.canWrite(
                                    ResolvableType.forClass(Payload.class), MediaType.APPLICATION_JSON
                            )).findFirst().orElseThrow();
                    assertThat(writer).isInstanceOf(EncoderHttpMessageWriter.class);
                    assertThat(((EncoderHttpMessageWriter<?>) writer).getEncoder()).isInstanceOf(
                            preferred.equals("default") ? JacksonJsonEncoder.class : Jackson2JsonEncoder.class
                    );
                    final String json = WebTestClient.bindToApplicationContext(context).build()
                            .get().uri("/json-contract").exchange().expectStatus().isOk()
                            .expectBody(String.class).returnResult().getResponseBody();
                    assertPayload(context.getBean(ObjectMapper.class), json);
                });
    }

    private static void assertPayload(ObjectMapper mapper, String json) throws Exception {
        final var node = mapper.readTree(json);
        assertThat(node.path("id").asText()).isEqualTo("9007199254740993");
        assertThat(node.path("id").isTextual()).isTrue();
        assertThat(node.path("amount").asText()).isEqualTo("12.30");
        assertThat(node.path("createdAt").asText()).isEqualTo("2026-11-12 10:20:30");
        assertThat(node.path("label").asText()).isEqualTo("kept");
        assertThat(node.path("phone").asText()).isEqualTo("138****5678");
    }

    @Configuration(proxyBeanMethods = false)
    static class EndpointConfiguration {
        @Bean
        Endpoint endpoint() {
            return new Endpoint();
        }
    }

    @RestController
    static class Endpoint {
        @GetMapping("/json-contract")
        Payload payload() {
            return new Payload();
        }
    }

    static class Payload {
        public final long id = 9007199254740993L;
        public final BigDecimal amount = new BigDecimal("12.30");
        public final LocalDateTime createdAt = LocalDateTime.of(2026, 11, 12, 10, 20, 30);
        public final Option<String> label = Option.of("kept");
        @PhoneDesensitize
        public final String phone = "13812345678";
    }
}
