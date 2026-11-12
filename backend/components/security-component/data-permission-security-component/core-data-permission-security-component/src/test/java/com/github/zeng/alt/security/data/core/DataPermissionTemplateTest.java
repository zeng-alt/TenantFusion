package com.github.zeng.alt.security.data.core;

import com.github.zeng.alt.security.data.DataPermissionRule;
import com.github.zeng.alt.security.data.DataPermissionScope;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * 验证授权并集、空授权拒绝以及加载失败不放行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionTemplateTest {
    private final DataPermissionSubject subject = new DataPermissionSubject(
            "t1", "7", Option.of(10L), Set.of("manager"), false);

    @Test
    void shouldUnionSelfDepartmentTreeAndExplicitDepartments() {
        final var template = template(List.of(
                new DataPermissionRule(DataPermissionScope.SELF, Set.of()),
                new DataPermissionRule(DataPermissionScope.DEPT, Set.of()),
                new DataPermissionRule(DataPermissionScope.DEPT_TREE, Set.of()),
                new DataPermissionRule(DataPermissionScope.DEPT_SET, Set.of(30L))));
        final var query = template.resource("user").permission("read").get().get();
        assertThat(query.self()).isTrue();
        assertThat(query.all()).isFalse();
        assertThat(query.departmentIds()).containsExactlyInAnyOrder(10L, 20L, 30L);
        assertThat(query.tenant()).isEqualTo("t1");
    }

    @Test
    void shouldDenyRowsWhenRulesOrDepartmentAreMissing() {
        final var query = template(List.of()).resource("user").permission("read").get().get();
        assertThat(query.all()).isFalse();
        assertThat(query.self()).isFalse();
        assertThat(query.departmentIds()).isEmpty();
        final var noDepartment = new DataPermissionTemplateImpl(p -> Either.right(new DataPermissionSubject(
                "t1", "7", Option.none(), Set.of("manager"), false)),
                (r, p, s) -> Either.right(List.of(new DataPermissionRule(DataPermissionScope.DEPT, Set.of()))),
                (t, d) -> Either.right(Set.of(d)), new DataPermissionProperties());
        assertThat(noDepartment.resource("user").permission("read").get().get().departmentIds()).isEmpty();
    }

    @Test
    void shouldKeepTenantBoundaryForAllGrant() {
        final var query = template(List.of(new DataPermissionRule(DataPermissionScope.ALL, Set.of())))
                .resource("user").permission("read").get().get();
        assertThat(query.all()).isTrue();
        assertThat(query.tenant()).isEqualTo("t1");
    }

    @Test
    void shouldPropagatePermissionAndPolicyFailures() {
        final var denied = new DataPermissionTemplateImpl(p -> Either.left("denied"),
                (r, p, s) -> Either.right(List.of()), (t, d) -> Either.right(Set.of()), new DataPermissionProperties());
        assertThat(denied.resource("user").permission("read").get().getLeft()).isEqualTo("denied");
        final var failed = new DataPermissionTemplateImpl(p -> Either.right(subject),
                (r, p, s) -> Either.left("policy unavailable"), (t, d) -> Either.right(Set.of()),
                new DataPermissionProperties());
        assertThat(failed.resource("user").permission("read").get().isLeft()).isTrue();
    }

    @Test
    void shouldRequireExplicitAdministratorAllAndStillCheckPolicies() {
        final var administrator = new DataPermissionSubject("t1", "7", Option.none(), Set.of(), true);
        final var properties = new DataPermissionProperties();
        final var template = new DataPermissionTemplateImpl(p -> Either.right(administrator),
                (r, p, u) -> Either.right(List.of()), (t, d) -> Either.right(Set.of()), properties);
        assertThat(template.resource("user").permission("read").get().get().all()).isFalse();
        properties.setAdministratorAll(true);
        assertThat(template.resource("user").permission("read").get().get().all()).isTrue();
        final var failure = new DataPermissionTemplateImpl(p -> Either.right(administrator),
                (r, p, u) -> Either.left("unsupported restriction"), (t, d) -> Either.right(Set.of()), properties);
        assertThat(failure.resource("user").permission("read").get().isLeft()).isTrue();
    }

    @Test
    void shouldPropagateDepartmentResolutionFailure() {
        final var template = new DataPermissionTemplateImpl(p -> Either.right(subject),
                (r, p, u) -> Either.right(List.of(new DataPermissionRule(DataPermissionScope.DEPT_TREE, Set.of()))),
                (t, d) -> Either.left("departments unavailable"), new DataPermissionProperties());
        assertThat(template.resource("user").permission("read").get().getLeft()).isEqualTo("departments unavailable");
    }

    private DataPermissionTemplate template(List<DataPermissionRule> rules) {
        return new DataPermissionTemplateImpl(p -> Either.right(subject),
                (r, p, s) -> Either.right(rules), (t, d) -> Either.right(Set.of(d, 20L)),
                new DataPermissionProperties());
    }
}
