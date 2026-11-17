package com.github.zeng.alt.log;

import com.github.zeng.alt.json.JsonConfiguration;
import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Servlet 参数、上传和过滤配置的集成契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LogServletTest {
    @AfterEach
    void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void includesQueryAndBodyWhileRemovingSecretsFromBoth() {
        runner().run(context -> {
            final var request = new MockHttpServletRequest("POST", "/users");
            request.addParameter("tag", "a", "b");
            request.addParameter("password", "query-secret");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            context.getBean(Actions.class).update(Map.of("name", "alice", "password", "body-secret"));
            final OperLogEvent event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getOperUrl()).isEqualTo("/users");
            assertThat(event.getRequestMethod()).isEqualTo("POST");
            assertThat(event.getOperIp()).isEqualTo("10.0.0.1");
            assertThat(event.getOperParam()).contains("alice", "tag").doesNotContain("secret", "password");
            final var parameters = context.getBean(JsonMapper.class).readTree(event.getOperParam());
            assertThat(parameters.get("tag").size()).isEqualTo(2);
        });
    }

    @Test
    void skipsRequestResponseUploadAndStreamArguments() {
        runner().run(context -> {
            final var request = new MockHttpServletRequest("POST", "/upload");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            context.getBean(Actions.class).upload(
                    request,
                    new MockHttpServletResponse(),
                    new MockMultipartFile("file", new byte[]{1, 2}),
                    new ByteArrayInputStream(new byte[]{1, 2})
            );
            assertThat(context.getBean(Records.class).events.getFirst().getOperParam()).isEqualTo("{}");
        });
    }

    @Test
    void appliesConfiguredExclusionsRecursivelyAndCaseInsensitively() {
        runner().withPropertyValues("alt.log.excluded-fields[0]=internalCode").run(context -> {
            context.getBean(Actions.class).update(Map.of("nested", Map.of("InternalCode", "hidden", "name", "kept")));
            assertThat(context.getBean(Records.class).events.getFirst().getOperParam())
                    .contains("kept").doesNotContain("InternalCode", "hidden");
        });
    }

    private WebApplicationContextRunner runner() {
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JsonConfiguration.class, JacksonAutoConfiguration.class, LogAutoConfiguration.class
        )).withUserConfiguration(Fixtures.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean Actions actions() { return new Actions(); }
        @Bean Records records() { return new Records(); }
    }

    public static class Actions {
        @Log
        public String update(Map<String, Object> body) { return "ok"; }
        @Log
        public void upload(
                HttpServletRequest request,
                HttpServletResponse response,
                MultipartFile file,
                InputStream stream
        ) { }
    }

    static class Records {
        final List<OperLogEvent> events = new ArrayList<>();
        @EventListener
        public void record(OperLogEvent event) { events.add(event); }
    }
}
