package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign 与 LoadBalancer 子上下文必须同时进入 AOT，且使用解析后的服务名。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class FeignAbacAotTest {
    @Test
    void generatesFeignAndLoadBalancerContextsForConfiguredServiceId() {
        try (final var context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of("security.abac.client.source=remote", "security.abac.client.service-id=policy-server",
                    "security.abac.client.client-id=workflow", "security.abac.client.service-key=test-key",
                    "security.abac.method.enabled=false", "spring.cloud.refresh.enabled=false").applyTo(context);
            context.register(Application.class);
            final var files = new InMemoryGeneratedFiles();
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(Application.class)), files);
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            generation.writeGeneratedContent();
            assertThat(files.getGeneratedFiles(GeneratedFiles.Kind.SOURCE).keySet())
                    .anyMatch(path -> path.contains("FeignClientFactory_abacPoliciesApplicationContextInitializer"))
                    .anyMatch(path -> path.contains("LoadBalancerClientFactory_policy_serverApplicationContextInitializer"));
            assertThat(generation.getRuntimeHints().reflection().getTypeHint(AbacPolicySnapshotDto.class)).isNotNull();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application { }
}
