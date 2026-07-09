package com.github.zeng.alt.domain.sort;

import com.github.zeng.alt.rest.annotation.QueryOrder;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image runtime hints for the auto-sort entity listener.
 * <p>
 * Registers reflection hints for:
 * <ul>
 *   <li>{@link AutoSortEntityListener} — so Hibernate can discover and invoke
 *       the {@code @PrePersist} method via the JPA lifecycle callback contract</li>
 *   <li>{@link QueryOrder} — so {@code field.getAnnotation(QueryOrder.class)}
 *       works at runtime in a native image</li>
 * </ul>
 * <p>
 * Entity classes that use {@link QueryOrder#autoSort()} = {@code true}
 * must have their fields registered for reflection as part of the regular
 * JPA entity hint registration (typically handled by Spring Data JPA's
 * own infrastructure).
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2026年07月07日
 */
public class AutoSortRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Entity listener — needs INVOKE_DECLARED_CONSTRUCTORS for Hibernate
        // instantiation via SpringBeanContainer, and
        // INVOKE_DECLARED_METHODS for the @PrePersist callback.
        hints.reflection().registerType(AutoSortEntityListener.class,
                MemberCategory.INTROSPECT_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        // QueryOrder annotation — required for field.getAnnotation() at runtime.
        hints.reflection().registerType(QueryOrder.class,
                MemberCategory.INTROSPECT_DECLARED_METHODS);
    }
}