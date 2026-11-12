package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.api.rest.PageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证原生用户分页、详情和角色 DTO 的实际序列化可达性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UserDataPermissionAotTest {
    @Test
    void shouldRegisterPaginationEntityAndNestedRoleBindings() {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(UserDataPermissionConfiguration.class);
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(UserDataPermissionAotTest.class)),
                    new InMemoryGeneratedFiles());
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            final var reflection = RuntimeHintsPredicates.reflection();
            assertThat(reflection.onMethod(PageEntity.class, "getTotal").test(generation.getRuntimeHints())).isTrue();
            assertThat(reflection.onMethod(PageEntity.class, "getPageData")
                    .test(generation.getRuntimeHints())).isTrue();
            assertThat(reflection.onMethod(User.class, "getUserId").test(generation.getRuntimeHints())).isTrue();
            assertThat(reflection.onMethod(UserDto.UserRoleDto.class, "getRole")
                    .test(generation.getRuntimeHints())).isTrue();
        }
    }
}
