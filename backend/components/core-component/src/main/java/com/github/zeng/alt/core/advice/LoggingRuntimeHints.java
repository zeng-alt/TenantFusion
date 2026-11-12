package com.github.zeng.alt.core.advice;

import ch.qos.logback.classic.BasicConfigurator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 允许 SLF4J 在 Native 运行期引导默认 Logback 配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class LoggingRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(BasicConfigurator.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
