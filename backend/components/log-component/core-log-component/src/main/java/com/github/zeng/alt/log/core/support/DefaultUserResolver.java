package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.security.api.UserContextHolder;
import io.vavr.control.Option;

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
        return Option.of(UserContextHolder.getUsername()).getOrElse("");
    }
}
