package com.github.zeng.alt.workflow;

import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.operaton.bpm.engine.ProcessEngine;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在隔离 Redis 和内存数据库上启动完整 workflow 宿主，验证框架及引擎装配。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Tag("docker")
@EnabledIfEnvironmentVariable(named = "SECURITY_DOCKER_TESTS", matches = "true")
@ActiveProfiles("upgrade-test")
@SpringBootTest(classes = WorkflowApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:workflow-upgrade;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.liquibase.enabled=false",
                "spring.data.redis.host=127.0.0.1",
                "alt.tenant.remote.enabled=false",
                "alt.camunda.identity.admin.enabled=false",
                "operaton.bpm.database.schema-update=true",
                "operaton.bpm.job-execution.enabled=false",
                "operaton.bpm.history-level=full"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class WorkflowApplicationTest {

    private final ServletWebServerApplicationContext context;
    private final ProcessEngine engine;

    WorkflowApplicationTest(ServletWebServerApplicationContext context, ProcessEngine engine) {
        this.context = context;
        this.engine = engine;
    }

    @DynamicPropertySource
    static void isolatedServices(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.port", () -> System.getenv("SECURITY_TEST_REDIS_PORT"));
        registry.add("security.jwt-auth.base64-secret", () -> "A".repeat(256));
        registry.add("security.jwt-auth.base64-secret-refresh", () -> "B".repeat(512));
    }

    @Test
    void startsActualWorkflowApplicationWithOperatonAndSecurity() throws Exception {
        assertThat(engine.getName()).isEqualTo("default");
        assertThat(context.getBean(ProcessDefinitionApi.class)).isNotNull();
        final var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + context.getWebServer().getPort() + "/upgrade-protected")).GET().build();

        final var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
