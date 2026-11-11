package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 远程租户源端到端：经真实 HTTP（JDK 自带 HttpServer 装 admin 端点）
 * 验证 Feign 声明、透传拦截器与 provider 缓存整条链路。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@SpringBootTest(classes = TenantFeignEndToEndTest.TestApp.class)
@TestPropertySource(properties = {
        "alt.tenant.remote.enabled=true",
        "alt.tenant.remote.base-url=http://127.0.0.1:${tenant.stub.port}",
        "alt.tenant.remote.service-token=svc-test",
        "alt.tenant.remote.cache-ttl=PT10M"})
class TenantFeignEndToEndTest {

    static final String T001_JSON = """
            {"tenantId":"t001","tenantName":"demo","mode":"SCHEMA",\
            "datasourceKey":null,"schemaName":"sch_t001","tableSuffix":null,\
            "rowIsolated":false,"enabled":true}""";

    static HttpServer stub;
    static int port;
    static final Map<String, String> seenHeaders = new ConcurrentHashMap<>();

    @BeforeAll
    static void startStub() throws IOException, InterruptedException {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/v1/tenants/t001", exchange -> {
            putIfPresent(seenHeaders, "Authorization",
                    firstOrNull(exchange.getRequestHeaders().get("Authorization")));
            putIfPresent(seenHeaders, "X-Tenant-Service-Key",
                    firstOrNull(exchange.getRequestHeaders().get("X-Tenant-Service-Key")));
            putIfPresent(seenHeaders, "X-Tenant-Id",
                    firstOrNull(exchange.getRequestHeaders().get("X-Tenant-Id")));
            byte[] body = T001_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        stub.createContext("/v1/tenants/unknown", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        stub.createContext("/v1/tenants", exchange -> {
            byte[] body = ("[" + T001_JSON + "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        stub.start();
        port = stub.getAddress().getPort();
        System.setProperty("tenant.stub.port", String.valueOf(port));
        // 预热：避免首个请求竞态
        java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("http://127.0.0.1:" + port + "/v1/tenants"))
                        .GET().build(),
                java.net.http.HttpResponse.BodyHandlers.discarding());
    }

    @AfterAll
    static void stopStub() {
        RequestContextHolder.resetRequestAttributes();
        System.clearProperty("tenant.stub.port");
        if (stub != null) {
            stub.stop(0);
        }
    }

    @Autowired
    RemoteTenantMetadataProvider provider;

    @Test
    @DisplayName("Feign 拉取元数据，无用户上下文时带服务密钥头")
    void fetchOverHttpWithServiceToken() {
        provider.evictAll();
        seenHeaders.clear();
        Optional<TenantMetadata> found = provider.findById("t001");

        assertThat(found).isPresent();
        assertThat(found.get().schemaName()).isEqualTo("sch_t001");
        assertThat(seenHeaders).doesNotContainKey("Authorization");
        assertThat(seenHeaders.get("X-Tenant-Service-Key")).isEqualTo("svc-test");
        assertThat(seenHeaders).doesNotContainKey("X-Tenant-Id");
    }

    @Test
    @DisplayName("用户请求的 JWT 被透传，覆盖 service-token")
    void forwardsUserJwtOverHttp() {
        provider.evictAll();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-jwt");
        request.addHeader("X-Tenant-Id", "t001");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertThat(provider.findById("t001")).isPresent();
            assertThat(seenHeaders.get("Authorization")).isEqualTo("Bearer user-jwt");
            assertThat(seenHeaders.get("X-Tenant-Id")).isEqualTo("t001");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("404 归一为空，全量透传")
    void notFoundMapsToEmpty() {
        assertThat(provider.findById("unknown")).isEmpty();

        List<TenantMetadata> all = provider.findAll();
        assertThat(all).extracting(TenantMetadata::tenantId).containsExactly("t001");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(RemoteTenantStoreAutoConfiguration.class)
    static class TestApp {
    }

    private static String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        } else {
            map.remove(key);
        }
    }
}
