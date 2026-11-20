package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.javapoet.ClassName;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Spring AOT 流程检查泛型 DTO 提示和构建期参数名资源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacMethodAotProcessorTest {
    @Test
    void generatesBindingHintsAndParameterNamesForAnnotatedMethodsOnly() throws Exception {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(Application.class);
            final var files = new InMemoryGeneratedFiles();
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(Application.class)), files);
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            final var hints = generation.getRuntimeHints();
            assertThat(RuntimeHintsPredicates.reflection().onMethod(GuardedService.class, "save").test(hints)).isTrue();
            assertThat(RuntimeHintsPredicates.reflection().onMethod(Input.class, "owner").test(hints)).isTrue();
            assertThat(RuntimeHintsPredicates.reflection().onMethod(Output.class, "accepted").test(hints)).isTrue();
            assertThat(RuntimeHintsPredicates.resource().forResource(AbacParameterNameResolver.RESOURCE).test(hints)).isTrue();
            final String resource = files.getGeneratedFileContent(GeneratedFiles.Kind.RESOURCE,
                    AbacParameterNameResolver.RESOURCE);
            assertThat(resource).contains("#save(java.util.List)=requests").doesNotContain("#plain");
            final Properties names = new Properties();
            names.load(new java.io.StringReader(resource));
            final var resolver = new AbacParameterNameResolver(names, new DefaultParameterNameDiscoverer() {
                @Override
                public String[] getParameterNames(java.lang.reflect.Method method) {
                    throw new AssertionError("Native parameter names must come from AOT resource");
                }
            });
            assertThat(resolver.getParameterNames(GuardedService.class.getMethod("save", List.class)))
                    .containsExactly("requests");
        }
    }

    @Test
    void skipsChildContextsWithoutAnnotatedMethods() {
        final var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        beans.registerBeanDefinition("plain", new org.springframework.beans.factory.support.RootBeanDefinition(Object.class));
        assertThat(new AbacMethodAotProcessor().processAheadOfTime(beans)).isNull();
    }

    @Test
    void fallsBackForJvmMethodsAndConstructors() throws Exception {
        final var resolver = new AbacParameterNameResolver(new Properties(), new DefaultParameterNameDiscoverer());
        assertThat(resolver.getParameterNames(GuardedService.class.getMethod("save", List.class)))
                .containsExactly("requests");
        assertThat(resolver.getParameterNames(Input.class.getDeclaredConstructor(String.class))).containsExactly("owner");
        final Properties names = new Properties();
        names.setProperty(AbacParameterNameResolver.key(GuardedService.class.getMethod("empty")), "");
        assertThat(new AbacParameterNameResolver(names, new DefaultParameterNameDiscoverer())
                .getParameterNames(GuardedService.class.getMethod("empty"))).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    static class Application {
        @Bean
        static BeanFactoryInitializationAotProcessor abacMethodAotProcessor() {
            return AbacMethodAutoConfiguration.abacMethodAotProcessor();
        }
        @Bean
        GuardedService service() { return new GuardedService(); }
    }

    static class GuardedService {
        @AbacAuthorize(permission = "save")
        public List<Output> save(List<Input> requests) { return List.of(new Output(true)); }
        public void plain() { }
        public void empty() { }
    }

    record Input(String owner) { }
    record Output(boolean accepted) { }
}
