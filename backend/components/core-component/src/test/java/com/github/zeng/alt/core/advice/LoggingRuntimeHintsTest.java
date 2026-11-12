package com.github.zeng.alt.core.advice;

import ch.qos.logback.classic.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Native 运行期可执行 SLF4J 默认配置器的反射构造。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoggingRuntimeHintsTest {

    @Test
    void permitsDefaultLogbackConfiguratorConstruction() throws Exception {
        final RuntimeHints hints = new RuntimeHints();
        new LoggingRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onConstructor(BasicConfigurator.class.getConstructor())
                .invoke().test(hints)).isTrue();
    }
}
