package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.annotation.Lock;
import com.github.zeng.alt.lock.aop.LockAnnotationAdvisor;
import com.github.zeng.alt.lock.aop.LockInterceptor;
import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.api.NoOpDistributedLock;
import com.github.zeng.alt.lock.api.NoOpLockTemplate;
import com.github.zeng.alt.lock.config.LockProperties;
import com.github.zeng.alt.lock.exception.LockException;
import com.github.zeng.alt.lock.exception.LockFailureException;
import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.*;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image runtime hints for the lock component.
 * <p>
 * Registers reflection hints for all lock-component classes
 * required to work correctly in a native image.
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 *
 */
public class LockRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // ========== Annotation ==========
        hints.reflection().registerType(Lock.class,
                MemberCategory.INTROSPECT_DECLARED_METHODS);
        hints.reflection().registerType(Lock.List.class,
                MemberCategory.INTROSPECT_DECLARED_METHODS);

        // ========== API / SPI interfaces ==========
        registerType(hints,
                DistributedLock.class,
                LockTemplate.class,
                LockExecutor.class,
                LockKeyBuilder.class,
                LockFailureStrategy.class);

        // ========== API implementations ==========
        registerType(hints,
                NoOpDistributedLock.class,
                NoOpLockTemplate.class);

        // ========== AOP ==========
        registerType(hints,
                LockAnnotationAdvisor.class,
                LockInterceptor.class);

        // ========== Model ==========
        registerType(hints,
                LockInfo.class,
                LockProperties.class,
                DefaultLockKeyBuilder.class,
                DefaultLockFailureStrategy.class,
                LockUtils.class);

        // ========== Exceptions ==========
        registerType(hints,
                LockException.class,
                LockFailureException.class);

        // ========== Executor ==========
        registerType(hints,
                AbstractLockExecutor.class);

        // ========== SpEL Evaluator ==========
        registerType(hints,
                SpelMethodBasedExpressionEvaluator.class,
                MethodBasedExpressionEvaluator.class);
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
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
