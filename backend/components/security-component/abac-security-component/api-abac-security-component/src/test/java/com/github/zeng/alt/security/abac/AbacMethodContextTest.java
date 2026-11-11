package com.github.zeng.alt.security.abac;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 方法上下文契约：实参数组与参数映射防御性拷贝，权限码透传。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacMethodContextTest {

    @Test
    void shouldDefensivelyCopyArgsAndParamMap() throws Exception {
        final Object[] args = {42L};
        final Map<String, Object> params = new HashMap<>(Map.of("id", 42L));
        final AbacMethodContext context = new AbacMethodContext(
                Object.class.getMethod("toString"), AbacMethodContextTest.class, args, params, "user:update");

        args[0] = 99L;
        params.put("id", 99L);

        assertThat(context.args()[0]).isEqualTo(42L);
        assertThat(context.paramByName().get("id")).isEqualTo(42L);
        assertThat(context.permission()).isEqualTo("user:update");
        assertThatThrownBy(() -> context.paramByName().put("x", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}