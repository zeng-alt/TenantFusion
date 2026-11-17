package com.github.zeng.alt.log;

import com.github.zeng.alt.json.JsonConfiguration;
import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import com.github.zeng.alt.log.core.handler.LogHandler;
import com.github.zeng.alt.log.core.support.UserResolver;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 操作结果、注解与 Jackson 3 记录契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LogBehaviorTest {
    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void recordsMetadataThroughAJdkProxyUsingTheImplementationAnnotation() {
        runner().run(context -> {
            TenantContextHolder.setTenantId("tenant-a");
            assertThat(context.getBean(Actions.class).perform(Map.of("name", "alice"))).isEqualTo("ok");
            final OperLogEvent event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getTitle()).isEqualTo("更新用户");
            assertThat(event.getTenantId()).isEqualTo("tenant-a");
            assertThat(event.getOperName()).isEqualTo("alice");
            assertThat(event.getStatus()).isEqualTo(BusinessStatus.SUCCESS.ordinal());
            assertThat(event.getOperTime()).isNotNull();
            assertThat(event.getMethod()).contains("ActionsImpl.perform");
            assertThat(event.getCostTime()).isNotNegative();
        });
    }

    @Test
    void controlsRequestAndResponseRecordingIndependently() {
        runner().run(context -> {
            final var actions = context.getBean(FlagActions.class);
            actions.requestOnly("input");
            actions.responseOnly("input");
            actions.neither("input");
            final var events = context.getBean(Records.class).events;
            assertThat(events).hasSize(3);
            assertThat(events.get(0).getOperParam()).contains("input");
            assertThat(events.get(0).getJsonResult()).isNull();
            assertThat(events.get(1).getOperParam()).isNull();
            assertThat(events.get(1).getJsonResult()).isEqualTo("\"output\"");
            assertThat(events.get(2).getOperParam()).isNull();
            assertThat(events.get(2).getJsonResult()).isNull();
        });
    }

    @Test
    void excludesFieldsFromNestedBodiesNamedArgumentsAndResponses() {
        runner().run(context -> {
            final var actions = context.getBean(FlagActions.class);
            actions.body(Map.of("visible", "kept", "nested", List.of(Map.of("privateField", "hidden"))), "secret");
            final var event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getOperParam()).contains("kept").doesNotContain("hidden", "secret");
            assertThat(event.getJsonResult()).contains("kept").doesNotContain("hidden", "secret");
        });
    }

    @Test
    void preservesBusinessResultsAndOriginalExceptionsWhenLoggingFails() {
        runner().withBean(LogHandler.class, () -> invocation -> { throw new IllegalStateException("sink failed"); })
                .run(context -> {
                    assertThat(context.getBean(Actions.class).perform(Map.of())).isEqualTo("ok");
                    assertThatThrownBy(() -> context.getBean(FlagActions.class).fail())
                            .isSameAs(FlagActions.FAILURE);
                });
    }

    @Test
    void recordsTheFailureWithoutChangingItsIdentity() {
        runner().run(context -> {
            assertThatThrownBy(() -> context.getBean(FlagActions.class).fail()).isSameAs(FlagActions.FAILURE);
            final var event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getStatus()).isEqualTo(BusinessStatus.FAIL.ordinal());
            assertThat(event.getErrorMsg()).isEqualTo("business failed");
        });
    }

    @Test
    void usesTheConfiguredJackson3Mapper() {
        runner().run(context -> {
            context.getBean(FlagActions.class).number();
            final var event = context.getBean(Records.class).events.getFirst();
            assertThat(context.getBean(JsonMapper.class).readTree(event.getJsonResult()).isString()).isTrue();
            assertThat(event.getJsonResult()).contains("9007199254740993");
        });
    }

    @Test
    void excludesSensitiveImplementationParameterNamesWhenTheInterfaceUsesAnotherName() {
        runner().run(context -> {
            context.getBean(Credentials.class).change("credential-value");
            assertThat(context.getBean(Records.class).events.getFirst().getOperParam())
                    .doesNotContain("credential-value");
        });
    }

    @Test
    void keepsMetadataWhenAResponseCannotBeSerialized() {
        runner().run(context -> {
            assertThat(context.getBean(FlagActions.class).unserializable()).isNotNull();
            final var event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getStatus()).isEqualTo(0);
            assertThat(event.getJsonResult()).isEmpty();
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JsonConfiguration.class, JacksonAutoConfiguration.class, LogAutoConfiguration.class
        )).withUserConfiguration(Fixtures.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean Actions actions() { return new ActionsImpl(); }
        @Bean Credentials credentials() { return new CredentialsImpl(); }
        @Bean FlagActions flags() { return new FlagActions(); }
        @Bean Records records() { return new Records(); }
        @Bean UserResolver userResolver() { return () -> "alice"; }
    }

    public interface Actions {
        String perform(Map<String, Object> body);
    }

    public static class ActionsImpl implements Actions {
        @Override
        @Log(title = "更新用户", businessType = BusinessType.UPDATE)
        public String perform(Map<String, Object> body) { return "ok"; }
    }

    public interface Credentials {
        void change(String value);
    }

    public static class CredentialsImpl implements Credentials {
        @Override
        @Log
        public void change(String password) { }
    }

    public static class Unserializable {
        public String getValue() { throw new IllegalStateException("unserializable value"); }
    }

    public static class FlagActions {
        static final RuntimeException FAILURE = new IllegalArgumentException("business failed");
        @Log(isSaveResponseData = false)
        public String requestOnly(String input) { return "output"; }
        @Log(isSaveRequestData = false)
        public String responseOnly(String input) { return "output"; }
        @Log(isSaveRequestData = false, isSaveResponseData = false)
        public String neither(String input) { return "output"; }
        @Log(excludeParamNames = {"privateField", "password"})
        public Map<String, Object> body(Map<String, Object> body, String password) {
            return Map.of("visible", "kept", "privateField", "hidden", "token", "secret");
        }
        @Log
        public void fail() { throw FAILURE; }
        @Log
        public Unserializable unserializable() { return new Unserializable(); }
        @Log
        public long number() { return 9007199254740993L; }
    }

    static class Records {
        final List<OperLogEvent> events = new ArrayList<>();
        @EventListener
        public void record(OperLogEvent event) { events.add(event); }
    }
}
