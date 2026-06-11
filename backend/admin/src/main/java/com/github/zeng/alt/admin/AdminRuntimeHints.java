package com.github.zeng.alt.admin;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.exception.BaseI18nException;
import com.github.zeng.alt.api.exception.UtilException;
import com.github.zeng.alt.api.rest.*;
import com.github.zeng.alt.lock.exception.LockException;
import com.github.zeng.alt.lock.exception.LockFailureException;
import com.github.zeng.alt.lock.model.LockProperties;
import com.github.zeng.alt.lock.model.LockInfo;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image runtime hints for the admin application.
 * <p>
 * Registers reflection, resource, and serialization hints required
 * for the entire application to work correctly in a native image.
 *
 * @author zengJiaJun
 * @since 2026骞?6鏈?1鏃? * @version 1.0
 */
public class AdminRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // ========== API Component ==========
        // BaseException uses Lombok @Getter - needs reflection for serialization
        registerReflection(hints,
                BaseException.class,
                BaseI18nException.class,
                UtilException.class,
                ErrorResponseEntity.class,
                RestResponse.class,
                PageRestResponse.class,
                PageResponseEntity.class,
                PageEntity.class,
                TreeRestResponse.class,
                TreeRestResponseEntity.class,
                TreeTableRestResponse.class,
                TurnPageRestResponse.class,
                TurnPageResponseEntity.class,
                TurnPageEntity.class,
                HttpEntity.class,
                HttpEntityStatus.class,
                ResponseEnum.class);

        // ========== Lock Component ==========
        registerReflection(hints,
                LockException.class,
                LockFailureException.class,
                LockProperties.class,
                LockInfo.class);

        // ========== Resource Patterns ==========
        // Register all Spring auto-configuration imports
        hints.resources().registerPattern("META-INF/spring/*.imports");
        hints.resources().registerPattern("META-INF/spring/*.factories");

        // Register application configuration files
        hints.resources().registerPattern("application.yml");
        hints.resources().registerPattern("application-dev.yml");
        hints.resources().registerPattern("application-prod.yml");
        hints.resources().registerPattern("messages*.properties");
        hints.resources().registerPattern("*.yaml");
        hints.resources().registerPattern("*.yml");

        // Liquibase changelog resources
        hints.resources().registerPattern("db/changelog/*.yaml");
        hints.resources().registerPattern("db/changelog/**/*.sql");

        // Logback configuration
        hints.resources().registerPattern("logback-spring.xml");
        hints.resources().registerPattern("org/springframework/boot/logging/logback/*.xml");
    }

    private void registerReflection(RuntimeHints hints, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS);
        }
    }
}
