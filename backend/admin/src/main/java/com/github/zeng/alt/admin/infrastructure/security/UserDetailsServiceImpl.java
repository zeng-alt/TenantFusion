package com.github.zeng.alt.admin.infrastructure.security;


import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.api.RoleGrantedAuthority;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.tenant.api.Tenant;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @since 2025年01月05日 14:39
 * @version 1.0
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final ObjectProvider<TenantSingleDataSourceProvider> tenantSingleDataSourceProvider;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        TenantSingleDataSourceProvider provider = tenantSingleDataSourceProvider.getIfAvailable();

        if (provider != null) {
            provider
                .findById(TenantContextHolder.getTenantId())
                .ifPresent(
                        t -> {
                            TenantContextHolder.setDatabase(t.getDb());
                            TenantContextHolder.setSchema(t.getSchema());
                        }
                );
        }

        return userRepository
                .findByUsername(username)
                .map(user -> {
                    Set<GrantedAuthority> roles = user.getUserRoles().stream().map(UserRole::getRole).map(r -> {
                        RoleGrantedAuthority authority = new RoleGrantedAuthority();
                        authority.setEnable(r.getEnabled());
                        authority.setCode(r.getCode());
                        authority.setName(r.getName());
                        return authority;
                    }).collect(Collectors.toSet());
                    Tenant tenant = provider != null ? provider.findById(user.getTenantBy()).orElse(new Tenant()) : new Tenant();
                    return SecurityUser
                            .withUsername(user.getUsername())
                            .id(user.getId())
                            .password(user.getPassword())
                            .tenant(user.getTenantBy())
                            .database(tenant.getDb())
                            .schema(tenant.getSchema())
                            .disabled(!user.getEnabled())
                            .roles(roles)
                            .currentRole(CollectionUtils.isEmpty(roles) ? null : roles.iterator().next())
                            .build();
                }).orElseThrow(() -> new UsernameNotFoundException(username + " 用户名不存在"));
    }
}
