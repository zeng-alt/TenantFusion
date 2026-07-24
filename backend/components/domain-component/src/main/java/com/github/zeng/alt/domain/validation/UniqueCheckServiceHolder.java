package com.github.zeng.alt.domain.validation;

import org.springframework.lang.Nullable;

/**
 * {@link IUniqueCheckRepository} 的静态持有者。由 {@link com.github.zeng.alt.json.JsonConfiguration}
 * 在容器启动时注入，支持在无实现时优雅降级（校验器直接通过）。
 */
public final class UniqueCheckServiceHolder {

    @Nullable
    private static volatile IUniqueCheckRepository repository;

    public UniqueCheckServiceHolder() {
    }

    public static void setRepository(@Nullable IUniqueCheckRepository repository) {
        UniqueCheckServiceHolder.repository = repository;
    }

    @Nullable
    public static IUniqueCheckRepository getRepository() {
        return repository;
    }
}
