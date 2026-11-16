package com.github.zeng.alt.config.client.properties;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import com.github.zeng.alt.config.client.ConfigClientAutoConfiguration;
import com.github.zeng.alt.config.client.ConfigSubscriber;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import jakarta.validation.constraints.Min;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 简化配置类的自动注册、宽松绑定与动态刷新测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigPropertiesIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void bindsNestedRecordsCollectionsAndLocalDefaultsWithoutFieldAnnotations() {
        runner().withPropertyValues("payment.timeout=7s").run(context -> {
            assertThat(context).hasNotFailed();
            final PaymentSettings settings = context.getBean(PaymentSettings.class);
            final Consumer consumer = context.getBean(Consumer.class);
            final ConfigSubscriber subscriber = context.getBean(ConfigSubscriber.class);
            assertThat(settings.isEnabled()).isFalse();
            assertThat(settings.getRules()).isEqualTo(new PaymentRules(3, "CNY"));
            assertThat(settings.getTimeout()).isEqualTo(Duration.ofSeconds(7));

            subscriber.acceptSnapshot(List.of(
                item("payment.enabled", "true", "text"),
                item("payment.rules", "{\"limit\":9,\"currency\":\"USD\"}", "json"),
                item("payment.methods", "[\"card\",\"cash\"]", "json"),
                item("payment.max-retries", "4", "text")
            ));
            assertThat(context.getBean(Consumer.class)).isSameAs(consumer);
            assertThat(consumer.settings.isEnabled()).isTrue();
            assertThat(settings.getRules()).isEqualTo(new PaymentRules(9, "USD"));
            assertThat(settings.getMethods()).containsExactly("card", "cash");
            assertThat(settings.getMaxRetries()).isEqualTo(4);
            subscriber.acceptSnapshot(List.of());
            assertThat(settings.isEnabled()).isFalse();
            assertThat(settings.getRules()).isEqualTo(new PaymentRules(3, "CNY"));
            assertThat(settings.getMethods()).containsExactly("card");
            assertThat(settings.getTimeout()).isEqualTo(Duration.ofSeconds(7));
        });
    }

    @Test
    void ignoresOtherGroupsAndNamespacesAndSupportsEmptyCollections() {
        runner().run(context -> {
            final PaymentSettings settings = context.getBean(PaymentSettings.class);
            final PaymentRules defaults = settings.getRules();
            final ConfigSubscriber subscriber = context.getBean(ConfigSubscriber.class);
            subscriber.acceptSnapshot(List.of(
                item("other.rules", "malformed unrelated JSON", "json"),
                new ConfigItemDTO("payment.enabled", "other", "true", "text")
            ));
            assertThat(settings.getRules()).isSameAs(defaults);
            subscriber.acceptSnapshot(List.of(item("payment.methods", "[]", "json")));
            assertThat(settings.getMethods()).isEmpty();
        });
    }

    @Test
    void validatesBoundPropertiesAndDoesNotExposeInvalidContentInErrors() {
        runner().run(context -> {
            final ConfigSubscriber subscriber = context.getBean(ConfigSubscriber.class);
            subscriber.acceptSnapshot(List.of(item("payment.max-retries", "0", "text")));
            assertThatThrownBy(() -> context.getBean(PaymentSettings.class).getMaxRetries())
                    .hasRootCauseInstanceOf(ConfigException.class);
            subscriber.acceptSnapshot(List.of(item("payment.rules", "secret invalid JSON", "json")));
            assertThatThrownBy(() -> context.getBean(PaymentSettings.class).getRules()).rootCause()
                    .isInstanceOf(ConfigException.class).hasMessageNotContaining("secret");
        });
    }

    @Test
    void letsSpecificPropertiesOverrideParentJsonDocuments() {
        runner().run(context -> {
            context.getBean(ConfigSubscriber.class).acceptSnapshot(List.of(
                item("payment.rules.limit", "12", "text"),
                item("payment", "{\"enabled\":false,\"rules\":{\"limit\":6,\"currency\":\"USD\"}}", "json"),
                item("payment.enabled", "true", "text")
            ));
            final PaymentSettings settings = context.getBean(PaymentSettings.class);
            assertThat(settings.isEnabled()).isTrue();
            assertThat(settings.getRules()).isEqualTo(new PaymentRules(12, "USD"));
        });
    }

    private ConfigItemDTO item(String key, String content, String format) {
        return new ConfigItemDTO(key, "DEFAULT_GROUP", content, format);
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class))
                .withUserConfiguration(ScanConfiguration.class)
                .withPropertyValues(
                    "config.client.app-code=shop",
                    "config.client.refresh-enabled=false",
                    "config.client.cache-dir=" + directory
                );
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = PaymentSettings.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ConfigProperties.class)
    )
    static class ScanConfiguration {
        @Bean
        Consumer consumer(PaymentSettings settings) {
            return new Consumer(settings);
        }
    }

    /**
     * 按字段名称绑定的业务配置。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Data
    @Validated
    @ConfigProperties(prefix = "payment")
    public static class PaymentSettings {
        private boolean enabled;
        private PaymentRules rules = new PaymentRules(3, "CNY");
        private List<String> methods = List.of("card");
        private Duration timeout = Duration.ofSeconds(5);
        @Min(1)
        private int maxRetries = 2;
    }

    /**
     * 嵌套不可变配置。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record PaymentRules(
        int limit,
        String currency
    ) {
    }

    private record Consumer(
        PaymentSettings settings
    ) {
    }
}
