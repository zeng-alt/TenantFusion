package com.github.zeng.alt.storage.redisson;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Native 中 Netty 初始化所需的 JDK 字段有明确的可达性提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RedissonRuntimeHintsTest {

    @Test
    void registersUnalignedFieldWithoutDependingOnNettyAnonymousClassNumber() {
        final RuntimeHints hints = new RuntimeHints();
        new RedissonRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(hints.reflection().getTypeHint(TypeReference.of("java.nio.Bits")).fields())
                .extracting(field -> field.getName()).containsExactly("UNALIGNED");
    }
}
