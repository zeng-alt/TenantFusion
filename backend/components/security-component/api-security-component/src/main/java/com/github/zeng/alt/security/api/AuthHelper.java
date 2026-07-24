package com.github.zeng.alt.security.api;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
public interface AuthHelper {

    default boolean isAdmin() {
        return isAdmin(UserContextHolder.getCurrentRole());
    }

    default boolean isSuperAdmin() {
        return isSuperAdmin(UserContextHolder.getId());
    }

    default boolean isSuperAdmin(Long id) {
        return isSuperAdmin(String.valueOf(id));
    }

    public boolean isAdmin(String code);

    public boolean isSuperAdmin(String id);
}
