package com.github.zeng.alt.admin.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.admin.AdminApplication;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.cookie.CookieAuthenticationFilter;
import com.github.zeng.alt.security.cookie.CookieAuthProperties;
import com.github.zeng.alt.security.cookie.CookieLoginHelper;
import com.github.zeng.alt.security.cookie.SessionManager;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.storage.StorageTemplate;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.springframework.web.servlet.view.xslt.XsltViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在隔离 PostgreSQL 与 Redis 上验证真实 admin HTTP 链、账户状态和权限缓存。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Tag("docker")
@EnabledIfEnvironmentVariable(named = "SECURITY_DOCKER_TESTS", matches = "true")
class AdminSecurityDockerTest {
    private static ServletWebServerApplicationContext context;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String PASSWORD = "security-review-test-only";
    private static JdbcTemplate jdbc;
    private static String address;

    @BeforeAll
    static void startApplication() {
        final String pg = "jdbc:postgresql://127.0.0.1:" + System.getenv("SECURITY_TEST_PG_PORT") + "/";
        final Map<String, String> properties = Map.ofEntries(
                Map.entry("spring.profiles.active", "prod"),
                Map.entry("server.port", "0"),
                Map.entry("spring.datasource.url", pg + "security_test"),
                Map.entry("spring.datasource.username", "postgres"),
                Map.entry("spring.datasource.password", "test-only"),
                Map.entry("spring.liquibase.enabled", "true"),
                Map.entry("spring.liquibase.contexts", "security-review"),
                Map.entry("spring.data.redis.host", "127.0.0.1"),
                Map.entry("spring.data.redis.port", System.getenv("SECURITY_TEST_REDIS_PORT")),
                Map.entry("spring.data.redis.database", "0"),
                Map.entry("alt.tenant.store.url", pg + "security_tenants"),
                Map.entry("alt.tenant.store.username", "postgres"),
                Map.entry("alt.tenant.store.password", "test-only"),
                Map.entry("alt.tenant.liquibase.enabled", "false"),
                Map.entry("security.jwt-auth.base64-secret", secret(192)),
                Map.entry("security.jwt-auth.base64-secret-refresh", secret(384)),
                Map.entry("security.captcha-auth.validation", "false"),
                Map.entry("security.rbac.client.enabled", "false"),
                Map.entry("oss.s3.endpoint", Path.of(System.getProperty("java.io.tmpdir"),
                        "tenantfusion-security-review-oss").toUri().toString()));
        context = (ServletWebServerApplicationContext) new SpringApplicationBuilder(AdminApplication.class)
                .run(properties.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue())
                        .toArray(String[]::new));
        address = "http://127.0.0.1:" + context.getWebServer().getPort();
        jdbc = new JdbcTemplate(context.getBean("dataSource", DataSource.class));
    }

    @BeforeEach
    void seedAccount() {
        TenantContextHolder.setTenantId("master");
        final String password = context.getBean(PasswordEncoder.class).encode(PASSWORD);
        jdbc.update("""
                insert into main_user (user_id, username, password, nick_name, tenant_by, is_enabled, is_deleted)
                values (900001, 'security-review', ?, 'original', 'master', true, false)
                on conflict (user_id) do update set password = excluded.password, is_enabled = true, nick_name = 'original'
                """, password);
        final var user = SecurityUser.withUsername("security-review").id("900001")
                .tenant("master").password("unused").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        context.getBean(LoginHelper.class).logout("900001");
        jdbc.update("delete from main_user_role where user_role_id = 900011");
        jdbc.update("delete from main_policy_rule where policy_rule_id = 900013");
        context.getBean(RbacResourceService.class).removeUserRole(List.of("900001"));
        context.getBean(RbacResourceService.class).removeRolePermission(List.of("security-review-role"));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterAll
    static void stopApplication() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void shouldLoginThroughHttpPersistRedisCredentialsAndRevokeBothTokens() throws Exception {
        grantAuthAdminPermission();
        final var login = login();
        final String access = context.getBean(ObjectMapper.class).readTree(login.body()).path("data")
                .path("accessToken").asText();
        final String refresh = login.headers().allValues("set-cookie").stream()
                .filter(cookie -> cookie.startsWith("refresh_token="))
                .map(cookie -> cookie.substring("refresh_token=".length(), cookie.indexOf(';')))
                .findFirst().orElseThrow();
        final var tokens = context.getBean(JwtTokenProvider.class);
        final var cache = context.getBean(JwtStorage.class);
        assertThat(cache.hasToken(tokens.getAccessCacheKey(access))).isTrue();
        assertThat(cache.hasToken(tokens.getRefreshCacheKey(refresh))).isTrue();
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(200);
        context.getBean(LoginHelper.class).logout("900001");
        assertThat(cache.hasToken(tokens.getAccessCacheKey(access))).isFalse();
        assertThat(cache.hasToken(tokens.getRefreshCacheKey(refresh))).isFalse();
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectCustomAuthenticatedRouteWithoutPermission() throws Exception {
        final String access = context.getBean(ObjectMapper.class).readTree(login().body()).path("data")
                .path("accessToken").asText();
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(403);
    }

    @Test
    void shouldApplyAbacPolicyToCustomAuthenticatedRoute() throws Exception {
        grantAuthAdminPermission();
        final String access = context.getBean(ObjectMapper.class).readTree(login().body()).path("data")
                .path("accessToken").asText();
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(200);
        jdbc.update("""
                insert into main_policy_rule
                    (policy_rule_id, permission_id, condition, is_pre_auth, tenant_by, is_deleted)
                values (900013, 10054, 'false', true, 'master', false)
                """);
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(403);
        jdbc.update("delete from main_policy_rule where policy_rule_id = 900013");
        assertThat(send("GET", "/v1/auth/admin", access, "").statusCode()).isEqualTo(200);
    }

    @Test
    void shouldBindRedisCookieToDatabaseTenantAndRejectDisabledAccount() throws Exception {
        final var sessions = new SessionManager(context.getBean(StorageTemplate.class), 86400);
        final var user = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String sessionId = sessions.createSession(user);
        final var filter = new CookieAuthenticationFilter(sessions, "SESSION", new CookieAuthProperties().getLogin(),
                context.getBean(UserDetailsService.class));
        final var request = new MockHttpServletRequest("GET", "/secured");
        request.setCookies(new Cookie("SESSION", sessionId));
        SecurityContextHolder.clearContext();
        TenantContextHolder.setTenantId("attacker-tenant");
        final var reached = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            reached.set(true);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("master");
            assertThat(context.getBean(UserRepository.class).findById(900001L).get().getTenantBy()).isEqualTo("master");
        });
        assertThat(reached).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
        jdbc.update("update main_user set is_enabled = false where user_id = 900001");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
        assertThat(sessions.getSession(sessionId)).isNull();
    }

    @Test
    void shouldRevokeAllUserCookieSessionsInRedisWithoutRevokingOtherTenant() {
        final var sessions = new SessionManager(context.getBean(StorageTemplate.class), 86400);
        final var user = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String first = sessions.createSession(user);
        final String second = sessions.createSession(user);
        final String foreign = sessions.createSession(SecurityUser.withUsername("foreign").id("900001")
                .tenant("foreign").password("unused").build());
        new CookieLoginHelper(context.getBean(AuthenticationManager.class), sessions, new CookieAuthProperties())
                .logout("900001");
        assertThat(sessions.getSession(first)).isNull();
        assertThat(sessions.getSession(second)).isNull();
        assertThat(sessions.getSession(foreign)).isNotNull();
        sessions.removeSession(foreign);
    }

    @Test
    void shouldRejectDisabledDatabaseAccountWithoutProfileMutation() throws Exception {
        final String access = context.getBean(ObjectMapper.class).readTree(login().body()).path("data")
                .path("accessToken").asText();
        jdbc.update("update main_user set is_enabled = false where user_id = 900001");
        assertThat(send("PATCH", "/v1/user/profile/900001", access, """
                {"nickName":"must-not-save"}
                """).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select nick_name from main_user where user_id = 900001", String.class))
                .isEqualTo("original");
    }

    @Test
    void shouldKeepOldComponentWhitelistPathsProtectedOverHttp() throws Exception {
        for (final String path : List.of("/tenant/graphql/query", "/h2-console/index", "/login/unrelated")) {
            assertThat(send("POST", path, "", "{}").statusCode()).as(path).isEqualTo(401);
        }
    }

    @Test
    void shouldRoundTripPartialRolePermissionsAndTenantCachesThroughRedis() {
        final var storage = context.getBean(StorageTemplate.class);
        final var rbac = context.getBean(RbacResourceService.class);
        final Duration ttl = Duration.ofMinutes(1);
        storage.opsForString().set("rbac:user:role:master:900001", new HashSet<>(Set.of("review-a", "review-b")), ttl);
        storage.opsForString().set("rbac:role:permissions:master:review-a", new HashSet<>(Set.of("review:read")), ttl);
        jdbc.update("""
                insert into main_role (role_id, code, tenant_by, is_enabled, is_deleted)
                values (900002, 'review-b', 'master', true, false) on conflict do nothing
                """);
        jdbc.update("""
                insert into main_permission (permission_id, resource_type, code, tenant_by, is_enabled, is_deleted)
                values (900003, 'HTTP', 'review:write', 'master', true, false) on conflict do nothing
                """);
        jdbc.update("""
                insert into main_role_permission (role_permission_id, role_id, permission_id, tenant_by, is_deleted)
                values (900004, 900002, 900003, 'master', false) on conflict do nothing
                """);
        assertThat(rbac.findRolePermission(List.of("review-a", "review-b"), "900001", "master"))
                .containsExactlyInAnyOrder("review:read", "review:write");
        storage.opsForString().set("rbac:user:role:t002:900001", new HashSet<>(Set.of("review-a")), ttl);
        storage.opsForString().set("rbac:role:permissions:t002:review-a", new HashSet<>(Set.of("tenant-b:read")), ttl);
        assertThat(rbac.findRolePermission(List.of("review-a"), "900001", "t002"))
                .containsExactly("tenant-b:read");
    }

    @Test
    void shouldNotEnableFeaturesRequiredByRemainingFrameworkAdvisories() {
        assertThat(context.getBeansOfType(XsltView.class)).isEmpty();
        assertThat(context.getBeansOfType(XsltViewResolver.class)).isEmpty();
        assertThat(context.getBeansOfType(RequestMappingHandlerMapping.class).values()).allSatisfy(mapping ->
                assertThat(mapping.getHandlerMethods().values()).noneMatch(handler ->
                        handler.getMethod().getGenericReturnType().getTypeName()
                                .matches(".*(FragmentsRendering|SseEmitter|ServerSentEvent).*")));
        final String router = "org.springframework.web.reactive.function.server.RouterFunction";
        if (ClassUtils.isPresent(router, context.getClassLoader())) {
            assertThat(context.getBeanNamesForType(ClassUtils.resolveClassName(router, context.getClassLoader())))
                    .isEmpty();
        }
    }

    private HttpResponse<String> login() throws Exception {
        final var response = send("POST", "/v1/login/jwt", "", """
                {"username":"security-review","password":"security-review-test-only","rememberMe":true}
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private void grantAuthAdminPermission() {
        jdbc.update("""
                insert into main_role (role_id, code, tenant_by, is_enabled, is_deleted)
                values (900010, 'security-review-role', 'master', true, false) on conflict do nothing
                """);
        jdbc.update("""
                insert into main_user_role (user_role_id, user_id, role_id, tenant_by, is_deleted)
                values (900011, 900001, 900010, 'master', false)
                """);
        jdbc.update("""
                insert into main_role_permission (role_permission_id, role_id, permission_id, tenant_by, is_deleted)
                values (900012, 900010, 10054, 'master', false) on conflict do nothing
                """);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        final var builder = HttpRequest.newBuilder(URI.create(address + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (!token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String secret(int size) {
        final byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
