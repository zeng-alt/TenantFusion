package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import io.vavr.control.Option;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * 本线程的查询权限快照；策略读取是组件内部的受控基础设施边界。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionContext {
    private static final ThreadLocal<Deque<Entry>> SCOPES = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> POLICY_READ = new ThreadLocal<>();

    private DataPermissionContext() { }

    static boolean policyRead() { return Boolean.TRUE.equals(POLICY_READ.get()); }

    static boolean active() { return SCOPES.get() != null && !SCOPES.get().isEmpty(); }

    static <T> T readPolicy(Supplier<T> supplier) {
        final boolean previous = policyRead();
        POLICY_READ.set(true);
        try {
            return supplier.get();
        } finally {
            if (previous) {
                POLICY_READ.set(true);
            } else {
                POLICY_READ.remove();
            }
        }
    }

    static Option<DataPermissionQuery> current(Class<?> entity) {
        return Option.of(SCOPES.get()).flatMap(scopes -> Option.ofOptional(scopes.stream()
                .filter(entry -> entry.entity() == entity).map(Entry::query).findFirst()));
    }

    static DataPermissionQuery require(Class<?> entity) {
        return current(entity).getOrElseThrow(() -> new AccessDeniedException(
                "Missing @DataPermission scope for " + entity.getName()));
    }

    static Scope open(Class<?> entity, DataPermissionQuery query) {
        current(entity).filter(previous -> !previous.equals(query)).peek(previous -> {
            throw new AccessDeniedException("Cannot change the data permission scope in a nested query");
        });
        if (SCOPES.get() == null) {
            SCOPES.set(new ArrayDeque<>());
        }
        SCOPES.get().push(new Entry(entity, query));
        return () -> {
            SCOPES.get().pop();
            if (SCOPES.get().isEmpty()) {
                SCOPES.remove();
            }
        };
    }

    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override void close();
    }

    private record Entry(Class<?> entity, DataPermissionQuery query) { }
}
