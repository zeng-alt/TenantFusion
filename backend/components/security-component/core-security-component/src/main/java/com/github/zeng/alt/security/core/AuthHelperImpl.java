package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
@RequiredArgsConstructor
public class AuthHelperImpl implements AuthHelper {

    private final SecurityProperties securityProperties;
    private final ObjectProvider<AuthorityResolver> authorityResolvers;

    @Override
    public boolean hasAdmin() {
        if (!Boolean.TRUE.equals(securityProperties.getAdmin().getEnabled())) {
            return false;
        }
        return hasSuperUser(UserContextHolder.getId()) || hasAdminRole(UserContextHolder.getCurrentRole());
    }

    @Override
    public boolean hasAdminRole(String code) {
        return StringUtils.hasText(code) && code.equalsIgnoreCase(securityProperties.getAdmin().getCode());
    }

    @Override
    public boolean hasSuperUser(String id) {
        return StringUtils.hasText(id) && Objects.equals(securityProperties.getAdmin().getId(), id);
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        final List<String> required = Arrays.asList(roles);
        return getAuthentication().exists(authentication -> getRoles(authentication).anyMatch(required::contains));
    }

    @Override
    public boolean hasAuthority(String authority) {
        return hasAnyAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        if (authorities.length == 0) {
            return false;
        }
        final List<String> required = Arrays.asList(authorities);
        return getAuthentication().exists(authentication -> Stream.concat(
                getRoles(authentication),
                authorityResolvers.orderedStream().flatMap(resolver -> resolver.getAuthorities(authentication).stream())
        ).anyMatch(required::contains));
    }

    private Option<Authentication> getAuthentication() {
        return Option.of(UserContextHolder.getAuthentication())
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken));
    }

    private Stream<String> getRoles(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUser user && user.getCurrentRole() != null) {
            return Stream.of(user.getCurrentRole().getAuthority());
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    }
}
