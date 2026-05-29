package com.github.zeng.alt.i18n;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image runtime hints for i18n component.
 * <p>
 * Registers reflection, resource, and serialization hints required
 * for the i18n module to work correctly in a native image.
 *
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2026年05月29日
 */
public class I18nRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register MessageSourceHelper for reflection (ApplicationContextAware)
        hints.reflection().registerType(
                MessageSourceHelper.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS
        );

        // Register MessageBaseNameProvider for reflection
        hints.reflection().registerType(
                MessageBaseNameProvider.class,
                MemberCategory.INVOKE_DECLARED_METHODS
        );

        // Register ResponseAdviceProvider for reflection
        hints.reflection().registerType(
                ResponseAdviceProvider.class,
                MemberCategory.INVOKE_DECLARED_METHODS
        );

        // Register LocaleConfiguration for reflection (used in auto-configuration)
        hints.reflection().registerType(
                LocaleConfiguration.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS
        );

        // Register resource patterns for i18n message bundles
        // ResourceBundleMessageSource loads .properties files at runtime
        hints.resources().registerPattern("messages*.properties");
        hints.resources().registerPattern("i18n/*.properties");
        hints.resources().registerPattern("ValidationMessages*.properties");

        // Register the auto-configuration imports file as a resource
        hints.resources().registerPattern("META-INF/spring/*.imports");
    }
}
