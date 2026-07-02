package com.github.zeng.alt.oss.core.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image RuntimeHints for the OSS config refresh module.
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class OssConfigRefreshRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(
                OssConfigRefreshAutoConfiguration.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS
        );
    }
}
