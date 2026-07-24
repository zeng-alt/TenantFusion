package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
@RequiredArgsConstructor
public class AuthHelperImpl implements AuthHelper {

    private final SecurityProperties securityProperties;

    @Override
    public boolean isAdmin(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        return code.equalsIgnoreCase(securityProperties.getAdmin().getCode()) || Objects.equals(securityProperties.getAdmin().getId(), code);
    }

    @Override
    public boolean isSuperAdmin(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        return Objects.equals(securityProperties.getAdmin().getId(), id);
    }
}
