package com.github.zeng.alt.camunda.engine.remote;

import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.remote.config.RemoteEngineAutoConfiguration;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Boot 4 Feign Starter 可独立调用与 Operaton 兼容的引擎 REST 协议。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RemoteEngineAutoConfigurationTest {

    @Test
    void loadsRemoteAdaptersAndDecodesProcessDefinitionWithoutEmbeddedEngine() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/engine-rest/process-definition/definition-1", exchange -> {
            final byte[] body = """
                    {"id":"definition-1","key":"approval","name":"Approval","version":1,
                     "deploymentId":"deployment-1","tenantId":"tenant-1","suspended":false}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (final var response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        server.start();
        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(RemoteEngineAutoConfiguration.class,
                            FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
                            JacksonAutoConfiguration.class))
                    .withPropertyValues("feign.client.config.default.url=http://127.0.0.1:"
                            + server.getAddress().getPort() + "/engine-rest")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        final var definition = context.getBean(ProcessDefinitionApi.class).get("definition-1");
                        assertThat(definition.getKey()).isEqualTo("approval");
                        assertThat(definition.getTenantId()).isEqualTo("tenant-1");
                    });
        } finally {
            server.stop(0);
        }
    }
}
