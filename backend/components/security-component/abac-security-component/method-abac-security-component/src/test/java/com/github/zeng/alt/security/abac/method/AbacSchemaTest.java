package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacAuthorize;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实方法目录提供补全、参数类型、结果类型和保存校验所需的同一份定义。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacSchemaTest {
    @Test
    void discoversBoundParametersAliasesResourceFieldsAndTransactionMetadata() {
        try (final var context = new AnnotationConfigApplicationContext(Application.class)) {
            final AbacSchemaDto schema = context.getBean(AbacMethodResolver.class).getSchema("user:update");
            assertThat(schema.methods()).hasSize(1);
            final AbacMethodSchemaDto method = schema.methods().getFirst();
            assertThat(method.transactional()).isTrue();
            assertThat(method.parameters()).anySatisfy(parameter -> {
                assertThat(parameter.name()).isEqualTo("dto");
                assertThat(parameter.index()).isEqualTo(1);
                assertThat(parameter.schema().fields()).anyMatch(field -> field.path().equals("#dto.deptId"));
            });
            assertThat(method.parameters().stream().map(AbacParameterDto::name)).contains("id", "p0", "a0", "dto", "p1", "a1");
            assertThat(method.returned().type()).contains("java.lang.Long");
            assertThat(schema.globals()).anySatisfy(field -> {
                assertThat(field.path()).isEqualTo("resource");
                assertThat(field.fields()).anyMatch(child -> child.path().equals("resource.deptId"));
            });
            assertThat(schema.functions()).contains("hasRole", "hasAuthority", "isAuthenticated");
        }
    }

    @Test
    void validatesTheActualStageAndRefusesUnknownOrHiddenFields() {
        try (final var context = new AnnotationConfigApplicationContext(Application.class)) {
            final AbacSchemaDto schema = context.getBean(AbacMethodResolver.class).getSchema("user:update");
            final var validator = new AbacExpressionValidator(context.getBean(SpelAbacEvaluator.class));
            assertThat(validator.validate("principal.deptId == resource.deptId and #dto.deptId == null", schema, true).isRight()).isTrue();
            assertThat(validator.validate("#p0 == #id", schema, true).isRight()).isTrue();
            assertThat(validator.validate("returnObject == #id", schema, false).isRight()).isTrue();
            assertThat(validator.validate("returnObject == #id", schema, true).getLeft().code()).isEqualTo("UNKNOWN_ATTRIBUTE");
            assertThat(validator.validate("#dto.password == 'x'", schema, true).isLeft()).isTrue();
            assertThat(validator.validate("#missing == null", schema, true).isLeft()).isTrue();
            assertThat(validator.validate("principal.unknown == null", schema, true).isLeft()).isTrue();
            assertThat(validator.validate("true", context.getBean(AbacMethodResolver.class)
                    .getSchema("unbound"), true).getLeft().code()).isEqualTo("METHOD_NOT_FOUND");
        }
    }

    @Test
    void resolvesNestedGenericReturnAndArrayFieldsWithoutExposingCredentials() {
        try (final var context = new AnnotationConfigApplicationContext(Application.class)) {
            final AbacSchemaDto schema = context.getBean(AbacMethodResolver.class).getSchema("user:read");
            final AbacFieldDto returned = schema.methods().getFirst().returned();
            assertThat(returned.type()).contains("List");
            final AbacFieldDto item = returned.fields().getFirst();
            assertThat(item.fields()).anyMatch(field -> field.path().equals("returnObject[0].deptId"));
            assertThat(item.fields()).noneMatch(field -> field.path().endsWith("password"));
            assertThat(item.fields()).anySatisfy(field -> {
                assertThat(field.path()).endsWith("tags");
                assertThat(field.fields()).anyMatch(child -> child.path().endsWith("tags[0]"));
            });
            final var validator = new AbacExpressionValidator(context.getBean(SpelAbacEvaluator.class));
            assertThat(validator.validate("returnObject[0].deptId == principal.deptId", schema, false).isRight()).isTrue();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AbacMethodAutoConfiguration.class)
    static class Application {
        @Bean
        SpelAbacEvaluator evaluator() { return new SpelAbacEvaluator(); }
        @Bean
        AbacTemplate template() { return context -> new AbacAuthorizationBuilder(required -> Either.right(true)); }
        @Bean
        AbacSubjectProvider subjects() {
            return permission -> Either.right(new AbacSubject("t", Map.of(), Map.of("authenticated", true)));
        }
        @Bean
        AbacEnvironmentProvider environment() { return context -> Map.of(); }
        @Bean
        AbacSchemaCustomizer schema() { return builder -> builder.field("principal", "deptId", Long.class); }
        @Bean
        TargetResolver resources() { return new TargetResolver(); }
        @Bean
        Users users() { return new Users(); }
    }

    static class Users {
        @Transactional
        @AbacAuthorize(permission = "user:update", resources = TargetResolver.class)
        public Either<String, Long> update(Long id, Change dto) { return Either.right(id); }
        @Transactional(readOnly = true)
        @AbacAuthorize(permission = "user:read")
        public Either<String, Option<List<Reply>>> read() { return Either.right(Option.none()); }
    }
    static class TargetResolver implements AbacResourceResolver {
        @Override
        public Map<String, Object> resolve(AbacMethodContext context) { return Map.of("deptId", 1L); }
        @Override
        public Map<String, Class<?>> attributes() { return Map.of("deptId", Long.class); }
    }
    record Change(Long deptId, String password) { }
    record Reply(Long deptId, String[] tags, String password) { }
}
