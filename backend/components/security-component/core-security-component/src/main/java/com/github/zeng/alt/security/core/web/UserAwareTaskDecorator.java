package com.github.zeng.alt.security.core.web;


import com.github.zeng.alt.security.api.UserContextHolder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年04月14日 17:37
 */
public class UserAwareTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        Authentication authentication = UserContextHolder.getAuthentication();
        return () -> {
            try {
                UserContextHolder.setAuthentication(authentication);
                runnable.run();
            } finally {
                UserContextHolder.clear();
            }
        };
    }
}
