package com.github.zeng.alt.config.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务配置读取与订阅行为测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigTemplateTest {
    @TempDir
    Path directory;

    @Test
    void appliesDefaultsOnlyToMissingValues() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(new ConfigItemDTO("enabled", "payment", "tru", "text")));
        final ConfigTemplate template = template(cache).group("payment");

        assertThat(template.opsForValue().getBoolean("missing", false).get()).isFalse();
        assertThat(template.opsForValue().getString("missing").isEmpty()).isTrue();
        assertThat(template.opsForValue().getBoolean("enabled", true).isLeft()).isTrue();
        assertThat(template.opsForValue().getBoolean("missing").getLeft()).contains("不存在");
    }

    @Test
    void readsScopedValuesAndBindsJsonRecords() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(
            new ConfigItemDTO("enabled", "payment", " TRUE ", "text"),
            new ConfigItemDTO("enabled", "delivery", "false", "text"),
            new ConfigItemDTO("rules", "payment", "{\"limit\":3}", "json")
        ));
        final ConfigTemplate template = template(cache);

        assertThat(template.group("payment").opsForValue().getBoolean("enabled").get()).isTrue();
        assertThat(template.group("delivery").opsForValue().getBoolean("enabled").get()).isFalse();
        assertThat(template.group("payment").opsForObject().get("rules", Rules.class).get()).isEqualTo(new Rules(3));
        assertThat(template.opsForValue().getString("enabled").isEmpty()).isTrue();
    }

    @Test
    void notifiesOnlyChangedKeysIncludingRemovalAndSupportsUnsubscribe() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        final var beans = new DefaultListableBeanFactory();
        final var properties = new ConfigClientProperties();
        properties.setAppCode("shop");
        final var listeners = new ConfigListenerTemplate(
            properties,
            beans.getBeanProvider(
                    ConfigChangeListener.class
            )
        );
        final var subscriber = new ConfigSubscriber(
            cache,
            event -> listeners.onConfigChange((ConfigChangeEvent) event),
            properties
        );
        final ConfigTemplate template = new ConfigTemplateImpl(cache, new JsonMapper(), listeners, "DEFAULT_GROUP");
        final var notifications = new AtomicInteger();
        final var registration = template.group("payment").subscribe(
            "enabled",
            event -> notifications.incrementAndGet()
        );
        final var item = new ConfigItemDTO("enabled", "payment", "true", "text");

        subscriber.acceptSnapshot(List.of(item));
        subscriber.acceptSnapshot(List.of(item));
        subscriber.acceptSnapshot(List.of(new ConfigItemDTO("enabled", "delivery", "false", "text"), item));
        subscriber.acceptSnapshot(List.of());
        assertThat(notifications).hasValue(2);

        registration.close();
        registration.close();
        subscriber.acceptSnapshot(List.of(item));
        assertThat(notifications).hasValue(2);
    }

    @Test
    void readsNumbersAndReportsInvalidJsonWithoutExposingContent() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(
            new ConfigItemDTO("count", "DEFAULT_GROUP", "42", "text"),
            new ConfigItemDTO("price", "DEFAULT_GROUP", "12.50", "text"),
            new ConfigItemDTO("invalid", "DEFAULT_GROUP", "secret malformed json", "json"),
            new ConfigItemDTO("empty", "DEFAULT_GROUP", "null", "json")
        ));
        final var config = template(cache);
        assertThat(config.opsForValue().getInteger("count").get()).isEqualTo(42);
        assertThat(config.opsForValue().getLong("count").get()).isEqualTo(42L);
        assertThat(config.opsForValue().getDecimal("price").get()).isEqualByComparingTo("12.50");
        assertThat(config.opsForValue().getInteger("missing", 3).get()).isEqualTo(3);
        assertThat(config.opsForValue().getLong("missing", 4L).get()).isEqualTo(4L);
        assertThat(config.opsForValue().getDecimal("missing", BigDecimal.ONE).get()).isEqualTo(BigDecimal.ONE);
        assertThat(config.opsForValue().getString("missing", "fallback")).isEqualTo("fallback");
        assertThat(config.opsForObject().get("invalid", Rules.class).getLeft()).doesNotContain("secret");
        assertThat(config.opsForObject().get("empty", Rules.class).isLeft()).isTrue();
    }

    private ConfigTemplate template(ConfigCacheManager cache) {
        final var properties = new ConfigClientProperties();
        properties.setAppCode("shop");
        final var beans = new DefaultListableBeanFactory();
        return new ConfigTemplateImpl(
            cache,
            new JsonMapper(),
            new ConfigListenerTemplate(
                properties,
                beans.getBeanProvider(ConfigChangeListener.class)
            ),
            "DEFAULT_GROUP"
        );
    }

    private record Rules(
        int limit
    ) {
    }
}
