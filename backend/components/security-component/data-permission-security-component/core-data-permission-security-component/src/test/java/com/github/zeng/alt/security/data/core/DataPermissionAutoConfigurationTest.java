package com.github.zeng.alt.security.data.core;

import com.github.zeng.alt.security.data.DataPermissionDepartmentResolver;
import com.github.zeng.alt.security.data.DataPermissionPolicyLoader;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import com.github.zeng.alt.security.data.DataPermissionSubjectResolver;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.core.Ordered;
import org.springframework.javapoet.ClassName;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证声明式配置、有序贡献及嵌套资源配置的原生绑定元数据。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionAutoConfigurationTest {
    @Test
    void shouldBindResourcesAndApplyCustomizersInOrder() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
                .withBean(DataPermissionSubjectResolver.class, () -> p -> Either.right(
                        new DataPermissionSubject("t1", "7", Option.none(), Set.of(), false)))
                .withBean(DataPermissionPolicyLoader.class, () -> (r, p, s) -> Either.right(List.of()))
                .withBean(DataPermissionDepartmentResolver.class, () -> (t, d) -> Either.right(Set.of(d)))
                .withBean("last", DataPermissionCustomizer.class, () -> new OrderedCustomizer(2, false))
                .withBean("first", DataPermissionCustomizer.class, () -> new OrderedCustomizer(1, true))
                .withPropertyValues("security.data-permission.resources.user.entity=java.lang.String",
                        "security.data-permission.resources.user.tenant=tenant",
                        "security.data-permission.resources.user.owner=ownerId",
                        "security.data-permission.resources.user.department=departmentId")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var properties = context.getBean(DataPermissionProperties.class);
                    assertThat(properties.isAdministratorAll()).isFalse();
                    assertThat(properties.getResources().get("user").getEntity()).isEqualTo(String.class);
                    assertThat(properties.getResources().get("user").getOwner()).isEqualTo("ownerId");
                });
    }

    @Test
    void shouldRegisterNativeBindingForResourceFields() {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(PropertiesConfiguration.class);
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(DataPermissionAutoConfigurationTest.class)),
                    new InMemoryGeneratedFiles());
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            assertThat(RuntimeHintsPredicates.reflection().onMethod(DataPermissionProperties.Resource.class, "setOwner")
                    .test(generation.getRuntimeHints())).isTrue();
        }
    }

    /**
     * 有序配置贡献者。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    private record OrderedCustomizer(int order, boolean administratorAll) implements DataPermissionCustomizer, Ordered {
        @Override
        public void customize(DataPermissionProperties properties) {
            properties.setAdministratorAll(administratorAll);
        }
        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * 仅生成属性元数据，隔离宿主服务。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataPermissionProperties.class)
    static class PropertiesConfiguration {
    }
}
