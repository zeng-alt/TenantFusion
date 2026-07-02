package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.security.api.UserContextHolder;

/**
 * 从 {@link UserContextHolder} 获取当前用户。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class DefaultUserResolver implements UserResolver {

    @Override
    public String currentUser() {
        try {
            return UserContextHolder.getUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
