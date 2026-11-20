package com.github.zeng.alt.security.abac;

import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 只读上下文树、方法阶段与事务状态契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacContextTest {
    @Test
    void freezesNestedValuesAndPreservesDeclaredNulls() {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("deptId", null);
        final List<Object> items = new ArrayList<>(Arrays.asList(nested, null));
        final var context = new AbacContext("t", "read", Map.of("profile", nested), Map.of("items", items), Map.of());
        nested.put("deptId", 10L);
        items.clear();
        assertThat(((Map<?, ?>) context.principal().get("profile")).containsKey("deptId")).isTrue();
        assertThat(((Map<?, ?>) context.principal().get("profile")).get("deptId")).isNull();
        assertThat((List<?>) context.resource().get("items")).hasSize(2);
        assertThatThrownBy(() -> context.principal().put("other", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void keepsEveryNumericScalarWithoutChangingItsType() {
        final List<Object> values = List.of("text", true, (byte) 1, (short) 2, 3, 4L, 5f, 6d,
                new BigDecimal("1.25"), BigInteger.TEN);
        final var context = new AbacContext("t", "p", Map.of("values", values), Map.of(), Map.of("hour", 9));
        assertThat(context.principal().get("values")).isEqualTo(values);
        assertThat(context.env()).containsEntry("hour", 9);
    }

    @Test
    void isolatesInvocationValuesAndExposesTransactionAndResultPhase() {
        final var original = new AbacContext("t", "p", Map.of(), Map.of(), Map.of());
        assertThat(original.invocation().preAuth()).isTrue();
        final var invocation = AbacInvocationDto.method(Map.of("authenticated", true), Map.of("id", 42L), true);
        final var context = original.withInvocation(invocation.after(Option.of(Map.of("allowed", true)), true));
        assertThat(context.tenant()).isEqualTo("t");
        assertThat(context.permission()).isEqualTo("p");
        assertThat(context.invocation().preAuth()).isFalse();
        assertThat(context.invocation().transactionActive()).isTrue();
        assertThat(context.invocation().authentication()).containsEntry("authenticated", true);
        assertThat(context.invocation().variables()).containsEntry("id", 42L);
        assertThat(context.invocation().returnObject().get()).isEqualTo(Map.of("allowed", true));
        assertThat(original.invocation().returnObject()).isEmpty();
    }

    @Test
    void rejectsObjectsNonStringKeysAndInvalidNamesAtTheBoundary() {
        assertThatThrownBy(() -> new AbacContext("", "p", Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t", "", Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t", "p", null, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t", "p", Map.of("", 1), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t", "p", Map.of("object", new Object()), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t", "p", Map.of("map", Map.of(1, "value")), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
