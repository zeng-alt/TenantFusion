package com.github.zeng.alt.security.abac.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Native 必须能够绑定服务账户 Map 中的凭据和允许租户集合。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacServicePropertiesAotTest {
    @Test
    void registersNestedServiceAccountBindingHints() throws Exception {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(Application.class);
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(Application.class)), new InMemoryGeneratedFiles());
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            assertThat(RuntimeHintsPredicates.reflection()
                    .onMethod(AbacServiceClientProperties.class.getMethod("setKey", String.class))
                    .test(generation.getRuntimeHints())).isTrue();
            assertThat(RuntimeHintsPredicates.reflection()
                    .onMethod(AbacServiceClientProperties.class.getMethod("setTenants", java.util.Set.class))
                    .test(generation.getRuntimeHints())).isTrue();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AbacServerProperties.class)
    static class Application { }
}
