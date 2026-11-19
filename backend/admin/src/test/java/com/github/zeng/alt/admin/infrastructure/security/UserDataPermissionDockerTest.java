package com.github.zeng.alt.admin.infrastructure.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.admin.AdminApplication;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 隔离 PostgreSQL/Redis 上验证用户数据权限的真实 HTTP、认证和数据库查询链。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Tag("docker")
@EnabledIfEnvironmentVariable(named = "SECURITY_DOCKER_TESTS", matches = "true")
class UserDataPermissionDockerTest {
    private static ServletWebServerApplicationContext context;
    private static JdbcTemplate jdbc;
    private static String address;
    private static final List<String> ROLES = List.of(
            "DP_SELF", "DP_DEPT", "DP_TREE", "DP_SET", "DP_ALL", "DP_EMPTY", "DP_DENIED");
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
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
                Map.entry("security.rbac.client.enabled", "true"),
                Map.entry("oss.s3.endpoint", Path.of(System.getProperty("java.io.tmpdir"),
                        "tenantfusion-security-review-oss").toUri().toString()));
        context = (ServletWebServerApplicationContext) new SpringApplicationBuilder(AdminApplication.class)
                .run(properties.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue())
                        .toArray(String[]::new));
        address = "http://127.0.0.1:" + context.getWebServer().getPort();
        jdbc = new JdbcTemplate(context.getBean("dataSource", DataSource.class));
    }


    @BeforeEach
    void seed() {
        new UserDataPermissionTestHelper(jdbc, context.getBean(RbacResourceService.class)).seed();
    }

    @AfterAll
    static void close() {
        if (context != null) {
            context.close();
        }
    }

    @ParameterizedTest
    @CsvSource({"DP_SELF,1,810001", "DP_DEPT,2,810001", "DP_TREE,3,810001", "DP_SET,1,810004", "DP_ALL,4,810001"})
    void shouldScopePaginationAndTotalsForCurrentRole(String role, int total, long first) throws Exception {
        final var response = send("GET", "/v1/user?username=dp-&sort=userId&order=asc&pageSize=1", token(role), "");
        assertThat(response.statusCode()).isEqualTo(200);
        final var page = json(response).path("data");
        assertThat(page.path("total").asInt()).isEqualTo(total);
        assertThat(page.path("pageData").size()).isEqualTo(1);
        assertThat(page.path("pageData").get(0).path("userId").asLong()).isEqualTo(first);
        assertThat(response.body()).doesNotContain("password", "dp-foreign");
    }

    @Test
    void shouldProtectDetailAndBothInfoLookupKeys() throws Exception {
        final String access = token("DP_SELF");
        assertThat(send("GET", "/v1/user/810001", access, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/v1/user/810003", access, "").statusCode()).isEqualTo(404);
        assertThat(send("GET", "/v1/user/810005", access, "").statusCode()).isEqualTo(404);
        assertThat(send("GET", "/v1/user/info?userId=810001", access, "").body()).contains("dp-reader");
        assertThat(send("GET", "/v1/user/info?username=dp-child", access, "").body()).doesNotContain("dp-child");
    }

    @Test
    void shouldReturnEmptyWithoutScopeAndDenyWithoutBasePermission() throws Exception {
        final var empty = send("GET", "/v1/user", token("DP_EMPTY"), "");
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json(empty).path("data").path("total").asLong()).isZero();
        assertThat(send("GET", "/v1/user", token("DP_DENIED"), "").statusCode()).isEqualTo(403);
        assertThat(send("GET", "/v1/role", token("DP_EMPTY"), "").statusCode()).isEqualTo(200);
    }

    @Test
    void shouldIntersectAdvancedSearchAndIgnoreOtherTenantRules() throws Exception {
        jdbc.update("""
                insert into main_data_permission
                  (data_permission_id, tenant_by, resource, permission, role_code, scope_type, is_enabled)
                values (879999, 't002', 'user', 'POST:/v1/user/search', 'DP_SELF', 'ALL', true)
                """);
        final var response = send("POST", "/v1/user/search", token("DP_SELF"),
                """
                {"filter":{"operator":"and","filters":[
                  {"operator":"contains","key":"username","value":"dp-"}]},
                 "options":{"pageSize":10,"pageOffset":0}}
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(response.body()).contains("dp-reader").doesNotContain("dp-foreign");
    }

    @Test
    void shouldApplyPolicyChangesWithoutWaitingForCacheExpiry() throws Exception {
        final String access = token("DP_SELF");
        jdbc.update("update main_data_permission set scope_type='DEPT' where role_code='DP_SELF'");
        assertThat(json(send("GET", "/v1/user?username=dp-", access, "")).path("data").path("total").asInt())
                .isEqualTo(2);
        jdbc.update("update main_data_permission set is_enabled=false where role_code='DP_SELF'");
        assertThat(json(send("GET", "/v1/user?username=dp-", access, "")).path("data").path("total").asInt())
                .isZero();
    }

    @Test
    void shouldDenyUnsupportedAbacRestrictionEvenWithAllScope() throws Exception {
        jdbc.update("""
                insert into main_policy_rule
                  (policy_rule_id, permission_id, condition, is_pre_auth, tenant_by, is_deleted)
                values (890001, 10049, 'true', true, 'master', false)
                """);
        assertThat(send("GET", "/v1/user", token("DP_ALL"), "").statusCode()).isEqualTo(403);
    }

    @Test
    void shouldRejectInvalidPaginationSortAndSensitiveSearchField() throws Exception {
        final String access = token("DP_SELF");
        assertThat(send("GET", "/v1/user?pageNo=0", access, "").statusCode()).isEqualTo(400);
        assertThat(send("GET", "/v1/user?sort=password", access, "").statusCode()).isEqualTo(400);
        final var response = send("POST", "/v1/user/search", access, """
                {"filter":{"operator":"and","filters":[
                  {"operator":"eq","key":"password","value":"not-a-secret"}]},
                 "options":{"pageSize":10,"pageOffset":0}}
                """);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldNotWidenSearchThroughNestedOr() throws Exception {
        final var response = send("POST", "/v1/user/search", token("DP_SELF"), """
                {"filter":{"operator":"or","filters":[
                  {"operator":"eq","key":"deptId","value":"830030"},
                  {"operator":"eq","key":"username","value":"dp-foreign"}]},
                 "options":{"pageSize":10,"pageOffset":0}}
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).path("data").path("total").asLong()).isZero();
    }

    @Test
    void shouldKeepGeneratedUserReadsDisabledAndIntersectOrdinaryFilters() throws Exception {
        assertThat(com.github.zeng.alt.admin.infrastructure.repository.UserHandler.class.getMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("list", "listAll", "detail", "search", "tree");
        final var response = send("GET", "/v1/user?deptId=830030&enabled=true&gender=M", token("DP_SELF"), "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).path("data").path("total").asLong()).isZero();
    }

    private String token(String role) {
        final var user = SecurityUser.withUsername("dp-reader").id("810001").tenant("master").password("unused")
                .roles(ROLES.stream().map(SimpleGrantedAuthority::new)
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a).toList())
                .currentRole(new SimpleGrantedAuthority(role)).build();
        final var provider = context.getBean(JwtTokenProvider.class);
        final String access = provider.createToken(user);
        context.getBean(JwtStorage.class).setAccessToken(provider.getAccessCacheKey(access), "dp-reader");
        return access;
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(address + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token).method(method, HttpRequest.BodyPublishers.ofString(body));
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(response.body());
    }

    private static String secret(int length) {
        final byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
