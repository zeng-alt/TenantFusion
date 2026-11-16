package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.annotation.ConfigBean;
import com.github.zeng.alt.config.annotation.ConfigListener;
import com.github.zeng.alt.config.annotation.ConfigValue;
import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注解注入、监听及已注入 Bean 的动态重建测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigAnnotationIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void refreshesInjectedProxiesBeforeNotifyingListeners() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class))
                .withUserConfiguration(BusinessConfiguration.class)
                .withPropertyValues(
                    "config.client.app-code=shop",
                    "config.client.refresh-enabled=false",
                    "config.client.cache-dir=" + directory
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var subscriber = context.getBean(ConfigSubscriber.class);
                    final var consumer = context.getBean(Consumer.class);
                    final var observer = context.getBean(Observer.class);
                    subscriber.acceptSnapshot(snapshot("true", "3"));
                    assertThat(consumer.flags.enabled()).isTrue();
                    assertThat(consumer.client.limit()).isEqualTo(3);
                    assertThat(observer.calls).hasValue(1);
                    assertThat(observer.observedLimit).isEqualTo(3);
                    assertThat(observer.globalCalls).hasValue(1);

                    subscriber.acceptSnapshot(snapshot("false", "5"));
                    assertThat(context.getBean(Consumer.class)).isSameAs(consumer);
                    assertThat(consumer.flags.enabled()).isFalse();
                    assertThat(consumer.client.limit()).isEqualTo(5);
                    assertThat(observer.calls).hasValue(2);
                    assertThat(observer.observedLimit).isEqualTo(5);
                    assertThat(observer.globalCalls).hasValue(2);
                });
    }

    private List<ConfigItemDTO> snapshot(String enabled, String limit) {
        return List.of(
            new ConfigItemDTO("enabled", "payment", enabled, "text"),
            new ConfigItemDTO("limit", "payment", limit, "text")
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessConfiguration {
        @Bean
        @ConfigBean(group = "payment")
        Flags flags() {
            return new Flags();
        }

        @Bean
        @ConfigBean(keys = "limit", group = "payment")
        PaymentClient paymentClient(ConfigTemplate config) {
            return new PaymentClient(config.group("payment").opsForValue().getInteger("limit", 0).get());
        }

        @Bean
        Consumer consumer(Flags flags, PaymentClient client) {
            return new Consumer(flags, client);
        }

        @Bean
        Observer observer(PaymentClient client) {
            return new Observer(client);
        }

        @Bean
        ConfigChangeListener globalListener(Observer observer) {
            return event -> observer.globalCalls.incrementAndGet();
        }
    }

    /**
     * 测试配置 Bean。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class Flags {
        @ConfigValue(value = "enabled", defaultValue = "false")
        private boolean enabled;

        public boolean enabled() {
            return enabled;
        }
    }

    /**
     * 测试配置 Bean。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class PaymentClient {
        private final int limit;

        PaymentClient(int limit) {
            this.limit = limit;
        }

        public int limit() {
            return limit;
        }
    }

    static class Consumer {
        private final Flags flags;
        private final PaymentClient client;

        Consumer(Flags flags, PaymentClient client) {
            this.flags = flags;
            this.client = client;
        }
    }

    static class Observer {
        private final PaymentClient client;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger globalCalls = new AtomicInteger();
        private int observedLimit;

        Observer(PaymentClient client) {
            this.client = client;
        }

        @ConfigListener(keys = {"enabled", "limit"}, group = "payment")
        public void changed(ConfigChangeEvent event) {
            calls.incrementAndGet();
            observedLimit = client.limit();
        }
    }
}
