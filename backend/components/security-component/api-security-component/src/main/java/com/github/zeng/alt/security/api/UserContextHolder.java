package com.github.zeng.alt.security.api;


import com.github.zeng.alt.tenant.api.TenantDetail;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年12月23日 21:57
 */
public final class UserContextHolder {

    private UserContextHolder() {}

    @Nullable
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (authentication.isAuthenticated()) {
            return authentication.getName();
        }

        return null;
    }

    @Nullable
    public static String getId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }

        return null;
    }

    @Nullable
    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return Optional.ofNullable(securityUser.getCurrentRole()).map(GrantedAuthority::getAuthority).orElse(null);
        }

        return null;
    }

    @Nullable
    public static UserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return (UserDetails) authentication.getPrincipal();
    }


    @Nullable
    public static String getTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof TenantDetail tenantDetail) {
            return tenantDetail.getTenantName();
        }

        return null;
    }

    @Nullable
    public static Authentication getAuthentication() {
        if (SecurityContextHolder.getContext() == null) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static void setAuthentication(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        } else {
            context.setAuthentication(authentication);
        }
    }

    @Nullable
    public static SecurityUser getSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser;
        }

        return null;
    }


    public static void set(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
