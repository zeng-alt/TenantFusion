package com.github.zeng.alt.security.data.core;

import com.github.zeng.alt.security.data.DataPermissionDepartmentResolver;
import com.github.zeng.alt.security.data.DataPermissionPolicyLoader;
import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionRule;
import com.github.zeng.alt.security.data.DataPermissionScope;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import com.github.zeng.alt.security.data.DataPermissionSubjectResolver;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import io.vavr.control.Either;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按授权并集合并范围；无规则产生空范围，错误不会变成全部数据。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionTemplateImpl implements DataPermissionTemplate {
    private final DataPermissionSubjectResolver subjects;
    private final DataPermissionPolicyLoader policies;
    private final DataPermissionDepartmentResolver departments;
    private final DataPermissionProperties properties;

    public DataPermissionTemplateImpl(
            DataPermissionSubjectResolver subjects,
            DataPermissionPolicyLoader policies,
            DataPermissionDepartmentResolver departments,
            DataPermissionProperties properties
    ) {
        this.subjects = subjects;
        this.policies = policies;
        this.departments = departments;
        this.properties = properties;
    }

    @Override
    public ResourceQuery resource(String resource) {
        return permission -> () -> subjects.resolve(permission).flatMap(subject ->
                policies.list(resource, permission, subject).flatMap(rules -> resolve(subject, rules)));
    }

    private Either<String, DataPermissionQuery> resolve(DataPermissionSubject subject, List<DataPermissionRule> rules) {
        if ((subject.administrator() && properties.isAdministratorAll())
                || rules.stream().anyMatch(rule -> rule.scope() == DataPermissionScope.ALL)) {
            return Either.right(new DataPermissionQuery(subject.tenant(), subject.userId(), true, false, Set.of()));
        }
        final boolean self = rules.stream().anyMatch(rule -> rule.scope() == DataPermissionScope.SELF);
        return Either.sequenceRight(rules.stream().map(rule -> departmentIds(subject, rule)).toList())
                .map(groups -> groups.flatMap(ids -> ids).toJavaStream().collect(Collectors.toUnmodifiableSet()))
                .map(ids -> new DataPermissionQuery(subject.tenant(), subject.userId(), false, self, ids));
    }

    private Either<String, Set<Long>> departmentIds(DataPermissionSubject subject, DataPermissionRule rule) {
        return switch (rule.scope()) {
            case DEPT_SET -> Either.right(rule.departmentIds());
            case DEPT -> Either.right(subject.departmentId().map(Set::of).getOrElse(Set::of));
            case DEPT_TREE -> subject.departmentId()
                    .map(id -> departments.descendants(subject.tenant(), id))
                    .getOrElse(() -> Either.right(Set.of()));
            case SELF, ALL -> Either.right(Set.of());
        };
    }
}
