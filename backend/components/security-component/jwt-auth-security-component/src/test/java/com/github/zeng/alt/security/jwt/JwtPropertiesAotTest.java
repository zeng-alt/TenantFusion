package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.LogoutProperties;
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
 * 验证原生程序可以绑定 JWT 的嵌套登录、登出路径配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtPropertiesAotTest {
    @Test
    void shouldGenerateBindingHintsForNestedLoginAndLogoutPaths() {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(PropertiesConfiguration.class);
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(JwtPropertiesAotTest.class)), new InMemoryGeneratedFiles());
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            final var reflection = RuntimeHintsPredicates.reflection();
            assertThat(reflection.onMethod(LoginProperties.class, "setLoginPath")
                    .test(generation.getRuntimeHints())).isTrue();
            assertThat(reflection.onMethod(LogoutProperties.class, "setLogoutPath")
                    .test(generation.getRuntimeHints())).isTrue();
            assertThat(reflection.onMethod(JwtProperties.class, "isSigningKeysValid")
                    .test(generation.getRuntimeHints())).isTrue();
        }
    }

    /**
     * 仅装配被测属性，隔离数据库和认证链。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfiguration {
    }
}
