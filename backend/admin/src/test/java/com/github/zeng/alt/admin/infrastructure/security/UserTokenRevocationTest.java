package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.query.api.UserQueryService;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.dto.PatchUserDto;
import com.github.zeng.alt.admin.query.service.UserSecurityService;
import com.github.zeng.alt.admin.query.service.UserServiceImpl;
import com.github.zeng.alt.admin.query.service.transformation.UserDtoTransformation;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.jwt.JwtAuthenticationFilter;
import com.github.zeng.alt.security.jwt.JwtLoginHelper;
import com.github.zeng.alt.security.jwt.JwtProperties;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.StorageTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 admin 停用操作使共享缓存中的凭证失效，只验签端也不能继续使用旧凭证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UserTokenRevocationTest {
    @AfterEach
    void clearContexts() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void shouldRevokeAccessAndRefreshAcrossValidatorsWhenDisablingUser() throws Exception {
        final JwtProperties properties = properties();
        final var tokens = new JwtTokenProvider(properties);
        final var storage = new JwtStorage(properties, storageTemplate());
        final var login = new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties);
        final SecurityUser principal = principal("7", "t001");
        final String access = tokens.createToken(principal);
        final String refresh = tokens.createRefreshToken(principal);
        final String otherUser = tokens.createToken(principal("8", "t001"));
        final String otherTenant = tokens.createToken(principal("7", "t002"));
        storage.setAccessToken(tokens.getAccessCacheKey(access), principal.getUsername());
        storage.setRefreshToken(tokens.getRefreshCacheKey(refresh), principal.getUsername());
        storage.setAccessToken(tokens.getAccessCacheKey(otherUser), "other-user");
        storage.setAccessToken(tokens.getAccessCacheKey(otherTenant), "other-tenant");

        final User disabled = disableUser(login);

        assertThat(disabled.getEnabled()).isFalse();
        assertThat(storage.hasToken(tokens.getAccessCacheKey(access))).isFalse();
        assertThat(storage.hasToken(tokens.getRefreshCacheKey(refresh))).isFalse();
        assertThat(storage.hasToken(tokens.getAccessCacheKey(otherUser))).isTrue();
        assertThat(storage.hasToken(tokens.getAccessCacheKey(otherTenant))).isTrue();
        UserContextHolder.clear();
        final var request = new MockHttpServletRequest("GET", "/secured");
        request.setServletPath("/secured");
        request.addHeader("Authorization", "Bearer " + access);
        request.setCookies(new Cookie(properties.getRefreshCookieName(), refresh));
        final var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(tokens, storage, Option.none(), properties).doFilter(request, response,
                (req, res) -> assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
        assertThat(response.getHeader(properties.getNewAccessTokenHeader())).isNull();
    }

    private User disableUser(JwtLoginHelper login) {
        final var users = mock(UserRepository.class);
        final var user = new User();
        user.setUserId(7L);
        user.setTenantBy("t001");
        when(users.findById(7L)).thenReturn(Option.of(user));
        final var service = new UserServiceImpl(
                users, mock(RoleRepository.class), mock(UserRoleRepository.class), mock(UserQueryService.class),
                new SecurityProperties(), mock(PasswordEncoder.class), Mappers.getMapper(UserDtoTransformation.class),
                mock(AuthHelper.class), new UserSecurityService(mock(RbacResourceService.class), login));
        final SecurityUser admin = principal("1", "t001");
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                admin, null, admin.getAuthorities()));
        TenantContextHolder.setTenantId("t001");
        final var command = new PatchUserDto();
        command.setEnabled(false);

        assertThat(service.patchUser(7L, command).get()).isEqualTo(7L);
        return user;
    }

    private StorageTemplate storageTemplate() {
        final Map<String, Object> entries = new HashMap<>();
        final CacheStringOperations strings = mock(CacheStringOperations.class);
        doAnswer(call -> {
            entries.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(strings).set(anyString(), any(), any(Duration.class));
        when(strings.hasKey(anyString())).thenAnswer(call -> entries.containsKey(call.getArgument(0)));
        when(strings.deleteByPattern(anyString())).thenAnswer(call -> {
            final String pattern = call.getArgument(0);
            final String prefix = pattern.substring(0, pattern.length() - 1);
            final long before = entries.size();
            entries.keySet().removeIf(key -> key.startsWith(prefix));
            return before - entries.size();
        });
        final StorageTemplate storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(strings);
        return storage;
    }

    private JwtProperties properties() {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        final var properties = new JwtProperties();
        properties.setBase64Secret(Base64.getEncoder().encodeToString(secret));
        return properties;
    }

    private SecurityUser principal(String id, String tenant) {
        return SecurityUser.withUsername("review-user").id(id).tenant(tenant).password("unused").roles().build();
    }
}
