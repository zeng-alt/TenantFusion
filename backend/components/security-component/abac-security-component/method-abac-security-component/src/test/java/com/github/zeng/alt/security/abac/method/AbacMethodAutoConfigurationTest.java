package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacTemplate;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 从 AutoConfiguration.imports 发现方法授权装配，验证默认 beans、退让、开关与缺 SPI 启动失败。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacMethodAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Application.class)
            .withBean(AbacTemplate.class, () -> mock(AbacTemplate.class))
            .withBean(AbacEnvironmentProvider.class, () -> context -> Map.of());

    @Test
    void shouldDiscoverMethodSecurityBeans() {
        runner.withBean(AbacSubjectProvider.class,
                        () -> permission -> Either.right(new AbacSubject("t001", Map.of())))
                .run(context -> {
                    assertThat(context).hasSingleBean(AbacAuthorizeAttributeRegistry.class);
                    assertThat(context).hasSingleBean(AbacMethodInterceptor.class);
                    assertThat(context).hasSingleBean(AbacMethodAdvisor.class);
                });
    }

    @Test
    void shouldBackOffWhenCustomAdvisorIsContributed() {
        runner.withBean(AbacSubjectProvider.class,
                        () -> permission -> Either.right(new AbacSubject("t001", Map.of())))
                .withBean(AbacMethodAdvisor.class, () -> mock(AbacMethodAdvisor.class))
                .run(context -> assertThat(context).hasSingleBean(AbacMethodAdvisor.class));
    }

    @Test
    void shouldDisableWhenPropertyIsFalse() {
        runner.withBean(AbacSubjectProvider.class,
                        () -> permission -> Either.right(new AbacSubject("t001", Map.of())))
                .withPropertyValues("security.abac.method.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AbacMethodInterceptor.class);
                    assertThat(context).doesNotHaveBean(AbacMethodAdvisor.class);
                });
    }

    @Test
    void shouldFailFastWithoutSubjectProvider() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
    }
}