package com.github.zeng.alt.i18n;

import com.github.zeng.alt.i18n.config.I18nAutoConfiguration;
import com.github.zeng.alt.i18n.config.I18nProperties;
import com.github.zeng.alt.i18n.core.DatabaseI18nMessageService;
import com.github.zeng.alt.i18n.core.DatabaseMessageSource;
import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.core.ResourceI18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import com.github.zeng.alt.i18n.repository.I18nMessageRepository;
import com.github.zeng.alt.i18n.rest.I18nMvcHandler;
import com.github.zeng.alt.i18n.rest.I18nWebFluxHandler;
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
        // ========== Configuration ==========
        registerType(hints,
                I18nAutoConfiguration.class,
                I18nProperties.class);

        // ========== Entity ==========
        registerType(hints,
                I18nMessageDO.class);

        // ========== Repository ==========
        registerType(hints,
                I18nMessageRepository.class);

        // ========== Service / MessageSource ==========
        registerType(hints,
                I18nMessageService.class,
                DatabaseMessageSource.class,
                DatabaseI18nMessageService.class,
                ResourceI18nMessageService.class);

        // ========== Handlers ==========
        registerType(hints,
                I18nMvcHandler.class,
                I18nWebFluxHandler.class);

        // ========== Utility / Provider ==========
        registerType(hints,
                MessageSourceHelper.class,
                MessageBaseNameProvider.class,
                ResponseAdviceProvider.class,
                LocaleConfiguration.class);

        // ========== Resource patterns for i18n message bundles ==========
        hints.resources().registerPattern("messages*.properties");
        hints.resources().registerPattern("i18n/*.properties");
        hints.resources().registerPattern("ValidationMessages*.properties");

        // Register the auto-configuration imports file as a resource
        hints.resources().registerPattern("META-INF/spring/*.imports");
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}


