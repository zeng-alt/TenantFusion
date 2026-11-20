package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.TenantSwitchTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证账户停用后，合法旧 JWT 和 Refresh Token 均不能恢复认证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtAuthenticationFilterTest {
    private final JwtStorage storage = mock(JwtStorage.class);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final Set<String> cachedKeys = new HashSet<>();
    private final JwtProperties properties = new JwtProperties();
    private final byte[] secret = new byte[192];
    private JwtTokenProvider tokens;

    @BeforeEach
    void setUp() {
        new SecureRandom().nextBytes(secret);
        properties.setBase64Secret(Base64.getEncoder().encodeToString(secret));
        tokens = new JwtTokenProvider(properties);
        when(storage.hasToken(anyString())).thenAnswer(call -> cachedKeys.contains(call.getArgument(0)));
        doAnswer(call -> {
            cachedKeys.add(call.getArgument(0));
            return null;
        }).when(storage).setAccessToken(anyString(), any());
    }

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired", "missing"})
    void shouldRejectExistingAccessForUnavailableAccount(String status) throws Exception {
        final String access = tokens.createToken(user("active"));
        cachedKeys.add(tokens.getAccessCacheKey(access));
        stubUser(status);

        assertThat(authenticate(request(access), new MockHttpServletResponse())).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired", "missing"})
    void shouldNotRefreshUnavailableAccount(String status) throws Exception {
        final String access = expiredAccess();
        final String refresh = tokens.createRefreshToken(user("active"));
        cachedKeys.add(tokens.getRefreshCacheKey(refresh));
        stubUser(status);
        final var request = request(access);
        request.setCookies(new Cookie(properties.getRefreshCookieName(), refresh));
        final var response = new MockHttpServletResponse();

        assertThat(authenticate(request, response)).isNull();
        assertThat(response.getHeader(properties.getNewAccessTokenHeader())).isNull();
    }

    @Test
    void shouldAuthenticateActiveAccountAndKeepSelectedRole() throws Exception {
        final String access = tokens.createToken(user("active"));
        cachedKeys.add(tokens.getAccessCacheKey(access));
        final var viewer = new SimpleGrantedAuthority("viewer");
        when(users.loadUserByUsername("review-user")).thenReturn(SecurityUser.withUsername("review-user")
                .id("7").tenant("t001").password("unused")
                .roles(viewer, new SimpleGrantedAuthority("operator")).currentRole(viewer).build());

        final Authentication authentication = authenticate(request(access), new MockHttpServletResponse());

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((SecurityUser) authentication.getPrincipal()).getPassword()).isNull();
        assertThat(((SecurityUser) authentication.getPrincipal()).getCurrentRole().getAuthority())
                .isEqualTo("operator");
        verify(users).loadUserByUsername("review-user");
    }

    @Test
    void shouldRejectTamperedSignatureBeforeLoadingUser() throws Exception {
        final String access = tokens.createToken(user("active"));
        final int signatureStart = access.lastIndexOf('.') + 1;
        final char replacement = access.charAt(signatureStart) == 'A' ? 'B' : 'A';
        final String tampered = access.substring(0, signatureStart) + replacement + access.substring(signatureStart + 1);

        assertThat(authenticate(request(tampered), new MockHttpServletResponse())).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void shouldRejectExpiredAccessWithoutRefreshCookie() throws Exception {
        assertThat(authenticate(request(expiredAccess()), new MockHttpServletResponse())).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void shouldRefreshActiveAccountAndStoreReplacementAccess() throws Exception {
        final String refresh = tokens.createRefreshToken(user("active"));
        cachedKeys.add(tokens.getRefreshCacheKey(refresh));
        when(users.loadUserByUsername("review-user")).thenReturn(user("active"));
        final var request = request(expiredAccess());
        request.setCookies(new Cookie(properties.getRefreshCookieName(), refresh));
        final var response = new MockHttpServletResponse();

        assertThat(authenticate(request, response)).isNotNull();
        final String replacement = response.getHeader(properties.getNewAccessTokenHeader());
        assertThat(replacement).isNotBlank();
        assertThat(cachedKeys).contains(tokens.getAccessCacheKey(replacement));
    }

    @Test
    void shouldRefreshUsingConfiguredApiHeaderOnlyWithBearerAccess() throws Exception {
        properties.setRefreshTokenHeader("X-Custom-Refresh");
        final String refresh = tokens.createRefreshToken(user("active"));
        cachedKeys.add(tokens.getRefreshCacheKey(refresh));
        stubUser("active");
        final var request = request(expiredAccess());
        request.addHeader(properties.getRefreshTokenHeader(), refresh);
        final var response = new MockHttpServletResponse();
        final var principal = new AtomicReference<Authentication>();
        final var filter = new JwtAuthenticationFilter(tokens, storage, Option.some(users), properties);
        filter.doFilter(request, response,
                (req, res) -> principal.set(SecurityContextHolder.getContext().getAuthentication()));
        assertThat(principal.get()).isNotNull();
        assertThat(response.getHeader(properties.getNewAccessTokenHeader())).isNotBlank();
        final var headerOnly = new MockHttpServletRequest("GET", "/secured");
        headerOnly.addHeader(properties.getRefreshTokenHeader(), refresh);
        filter.doFilter(headerOnly, new MockHttpServletResponse(),
                (req, res) -> assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
    }

    @Test
    void switchesOnlyAfterVerifyingOriginalIdentity() throws Exception {
        final String access = tokens.createToken(user("active"));
        cachedKeys.add(tokens.getAccessCacheKey(access));
        when(users.loadUserByUsername("review-user")).thenAnswer(call -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("t001");
            return user("active");
        });
        final var request = request(access);
        request.addHeader("X-Active-Tenant", "t002");
        final var response = new MockHttpServletResponse();
        final var filter = new JwtAuthenticationFilter(tokens, storage, Option.some(users), properties,
                Option.some(switchTemplate("7", "t001")));

        filter.doFilter(request, response, (req, res) -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("t002");
            assertThat(((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getTenant())
                    .isEqualTo("t001");
        });

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void exposesRequestHeadersDuringRemoteTenantValidation() throws Exception {
        final String access = tokens.createToken(user("active"));
        cachedKeys.add(tokens.getAccessCacheKey(access));
        when(users.loadUserByUsername("review-user")).thenReturn(user("active"));
        final var request = request(access);
        request.addHeader("X-Active-Tenant", "t002");
        final var template = mock(TenantSwitchTemplate.class);
        when(template.select(any(), anyString())).thenAnswer(call -> {
            final var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            assertThat(attributes.getRequest().getHeader("Authorization")).isEqualTo("Bearer " + access);
            assertThat(attributes.getRequest().getHeader("X-Active-Tenant")).isEqualTo("t002");
            return io.vavr.control.Either.right("t002");
        });
        new JwtAuthenticationFilter(tokens, storage, Option.some(users), properties, Option.some(template))
                .doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    assertThat(RequestContextHolder.getRequestAttributes()).isNull();
                    assertThat(TenantContextHolder.getTenantId()).isEqualTo("t002");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-id", "wrong-tenant", "unknown"})
    void rejectsUnauthorizedOrUnknownTenant(String reason) throws Exception {
        final String access = tokens.createToken(user("active"));
        cachedKeys.add(tokens.getAccessCacheKey(access));
        when(users.loadUserByUsername("review-user")).thenReturn(user("active"));
        final var request = request(access);
        request.addHeader("X-Active-Tenant", reason.equals("unknown") ? "missing" : "t002");
        final var response = new MockHttpServletResponse();
        final var filter = new JwtAuthenticationFilter(tokens, storage, Option.some(users), properties,
                Option.some(switchTemplate(reason.equals("wrong-id") ? "1" : "7",
                        reason.equals("wrong-tenant") ? "master" : "t001")));

        filter.doFilter(request, response, (req, res) -> { throw new AssertionError("request was not rejected"); });

        assertThat(response.getStatus()).isEqualTo(403);
    }

    private TenantSwitchTemplate switchTemplate(String id, String tenant) {
        final var security = new SecurityProperties();
        security.getAdmin().setId(id);
        security.getAdmin().setTenant(tenant);
        final var metadata = mock(TenantMetadataProvider.class);
        when(metadata.findCurrentById("t002")).thenReturn(Optional.of(
                new TenantMetadata("t002", "租户二", null, null, null, null, null, true)));
        @SuppressWarnings("unchecked")
        final ObjectProvider<TenantMetadataProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(metadata);
        return new TenantSwitchTemplate(security, provider);
    }

    private Authentication authenticate(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        final var authentication = new AtomicReference<Authentication>();
        new JwtAuthenticationFilter(tokens, storage, users, "Authorization",
                LoginProperties.of("/login/jwt", HttpMethod.POST), properties.getNewAccessTokenHeader(),
                properties.getRefreshCookieName()).doFilter(request, response,
                        (req, res) -> authentication.set(SecurityContextHolder.getContext().getAuthentication()));
        return authentication.get();
    }

    private void stubUser(String status) {
        if ("missing".equals(status)) {
            when(users.loadUserByUsername("review-user")).thenThrow(new UsernameNotFoundException("missing"));
            return;
        }
        when(users.loadUserByUsername("review-user")).thenReturn(user(status));
    }

    private SecurityUser user(String status) {
        final var role = new SimpleGrantedAuthority("operator");
        return SecurityUser.withUsername("review-user").id("7").tenant("t001").password("unused")
                .roles(role).currentRole(role).disabled("disabled".equals(status))
                .accountLocked("locked".equals(status)).accountExpired("expired".equals(status))
                .credentialsExpired("credentialsExpired".equals(status)).build();
    }

    private String expiredAccess() {
        final Instant now = Instant.now();
        final var claims = JwtClaimsSet.builder().id(UUID.randomUUID().toString()).subject("review-user")
                .claim("id", "7").claim("tenant", "t001").claim("roles", List.of("operator"))
                .claim("currentRole", "operator").issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600)).build();
        final var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret, "HmacSHA256")));
        final String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertThat(tokens.parseAccessToken(access).result()).isEqualTo(JwtTokenProvider.TokenValidationResult.EXPIRED);
        return access;
    }

    private MockHttpServletRequest request(String access) {
        final var request = new MockHttpServletRequest("GET", "/secured");
        request.setServletPath("/secured");
        request.addHeader("Authorization", "Bearer " + access);
        return request;
    }
}
