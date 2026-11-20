package com.github.zeng.alt.security.abac.method;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 参数和成功返回值投影，拒绝原始 Java 对象并隐藏凭据字段。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacValueResolverTest {
    private final AbacValueResolver resolver = new AbacValueResolver();

    @Test
    void projectsRecordsNestedValuesAndUnwrapsSuccessfulContainers() {
        final var reply = new Reply(42L, List.of("one"), new String[]{"two"}, "hidden");
        final var projected = (Map<?, ?>) resolver.resolve(Either.right(Option.of(reply))).get();
        assertThat(projected.get("id")).isEqualTo(42L);
        assertThat(projected.get("names")).isEqualTo(List.of("one"));
        assertThat(projected.get("tags")).isEqualTo(List.of("two"));
        assertThat(projected.containsKey("password")).isFalse();
        assertThat(resolver.resolve(Either.left("business failure"))).isEmpty();
        assertThat(resolver.resolve(Option.none())).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void projectsBeanGettersAndScalarKinds() {
        assertThat(((Map<?, ?>) resolver.resolve(new Bean()).get()).get("enabled")).isEqualTo(true);
        assertThat(resolver.resolve(Map.of("items", List.of(1L, 'a', State.READY))).get())
                .isEqualTo(Map.of("items", List.of(1L, "a", "READY")));
        assertThat(resolver.resolve(LocalDate.of(2026, 11, 20)).get()).isEqualTo("2026-11-20");
        final UUID id = UUID.randomUUID();
        assertThat(resolver.resolve(id).get()).isEqualTo(id.toString());
        assertThatThrownBy(() -> resolver.resolve(Object.class)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.resolve(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    public record Reply(Long id, List<String> names, String[] tags, String password) { }
    public static class Bean { public boolean isEnabled() { return true; } }
    enum State { READY }
}
