package com.github.zeng.alt.log;

import com.github.zeng.alt.json.JsonConfiguration;
import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import com.github.zeng.alt.log.core.handler.LogRecordFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Servlet、非 Web 和 WebFlux 下的装配契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LogAutoConfigurationTest {
    private static final AutoConfigurations CONFIGURATIONS = AutoConfigurations.of(
            JsonConfiguration.class, JacksonAutoConfiguration.class, LogAutoConfiguration.class
    );

    @Test
    void startsWithoutAServletContainer() {
        new ApplicationContextRunner().withConfiguration(CONFIGURATIONS)
                .withClassLoader(new FilteredClassLoader("jakarta.servlet"))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(LogRecordFactory.class)
                        .hasSingleBean(JsonMapper.class));
    }

    @Test
    void startsInWebFlux() {
        new ReactiveWebApplicationContextRunner().withConfiguration(CONFIGURATIONS)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(LogRecordFactory.class));
    }

    @Test
    void startsInServletApplications() {
        new WebApplicationContextRunner().withConfiguration(CONFIGURATIONS)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(LogRecordFactory.class));
    }
}
