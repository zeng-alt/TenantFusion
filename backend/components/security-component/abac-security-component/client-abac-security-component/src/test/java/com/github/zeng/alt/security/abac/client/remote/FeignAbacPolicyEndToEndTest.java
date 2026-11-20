package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.sun.net.httpserver.HttpServer;
import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真正的 Feign、服务发现和负载均衡 HTTP 调用，前台与后台均使用独立服务身份。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class FeignAbacPolicyEndToEndTest {
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> clientId = new AtomicReference<>();
    private final AtomicReference<String> serviceKey = new AtomicReference<>();
    private final AtomicReference<String> tenant = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);

    @Test
    void discoversServiceAndNeverInheritsUserAuthorization() throws Exception {
        final HttpServer server = policyServer();
        server.start();
        try {
            new ApplicationContextRunner().withUserConfiguration(Application.class).withPropertyValues(
                    "security.abac.client.source=remote", "security.abac.client.service-id=policy-server",
                    "security.abac.client.client-id=workflow", "security.abac.client.service-key=test-key",
                    "security.abac.method.enabled=false", "spring.cloud.refresh.enabled=false",
                    "spring.cloud.discovery.client.simple.instances.policy-server[0].uri=http://127.0.0.1:"
                            + server.getAddress().getPort())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        final AbacPolicyQueryService source = context.getBean(AbacPolicyQueryService.class);
                        final var userRequest = new MockHttpServletRequest();
                        userRequest.addHeader("Authorization", "Bearer unrelated-user");
                        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(userRequest));
                        try {
                            assertThat(source.get(new AbacPolicyQuery("t001", "update")).isRight()).isTrue();
                        } finally {
                            RequestContextHolder.resetRequestAttributes();
                        }
                        assertThat(authorization.get()).isNull();
                        assertThat(clientId.get()).isEqualTo("workflow");
                        assertThat(serviceKey.get()).isEqualTo("test-key");
                        assertThat(tenant.get()).isEqualTo("t001");
                        assertThat(context.getBean(AtomicInteger.class)).hasValue(0);
                        assertThat(source.get(new AbacPolicyQuery("t001", "update")).isRight()).isTrue();
                        assertThat(authorization.get()).isNull();
                        status.set(401);
                        assertThat(source.get(new AbacPolicyQuery("t001", "update")).getLeft().code())
                                .isEqualTo("POLICY_REMOTE_FAILED");
                    });
        } finally {
            RequestContextHolder.resetRequestAttributes();
            server.stop(0);
        }
    }

    private HttpServer policyServer() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(AbacPolicyQueryService.PATH, exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            clientId.set(exchange.getRequestHeaders().getFirst(AbacPolicyQueryService.CLIENT_HEADER));
            serviceKey.set(exchange.getRequestHeaders().getFirst(AbacPolicyQueryService.KEY_HEADER));
            tenant.set(exchange.getRequestHeaders().getFirst(AbacPolicyQueryService.TENANT_HEADER));
            final byte[] body = """
                    {"tenant":"t001","permission":"update","revision":"empty","policies":[]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), body.length);
            try (final var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        return server;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
        @Bean
        AtomicInteger inheritedInterceptorCalls() {
            return new AtomicInteger();
        }

        @Bean
        RequestInterceptor unrelatedFeignUserInterceptor(AtomicInteger inheritedInterceptorCalls) {
            return template -> {
                inheritedInterceptorCalls.incrementAndGet();
                template.header("Authorization", "Bearer global-user");
            };
        }
    }
}
