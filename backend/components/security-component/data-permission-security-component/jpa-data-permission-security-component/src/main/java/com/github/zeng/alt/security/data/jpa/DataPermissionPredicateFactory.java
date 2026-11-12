package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import jakarta.persistence.EntityManager;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.jpa.domain.Specification;

/**
 * 将同一数据范围编译为 Querydsl 和 Criteria 条件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionPredicateFactory<T> {
    private final DataPermissionMapping mapping;
    private final PathBuilder<T> root;
    private final Class<?> ownerType;
    private final String id;
    private final Class<?> idType;

    DataPermissionPredicateFactory(EntityManager entityManager, DataPermissionMapping mapping, PathBuilder<T> root) {
        this.mapping = mapping;
        this.root = root;
        final var entity = entityManager.getMetamodel().entity(root.getType());
        ownerType = entity.getSingularAttribute(mapping.owner()).getJavaType();
        final var idAttribute = entity.getSingularAttributes().stream().filter(a -> a.isId()).findFirst().orElseThrow();
        id = idAttribute.getName();
        idType = idAttribute.getJavaType();
    }

    Predicate querydsl(DataPermissionQuery scope) {
        final var visible = scope.all() ? Expressions.TRUE : Expressions.FALSE
                .or(scope.self() ? equality(mapping.owner(), ownerValue(scope)) : Expressions.FALSE)
                .or(scope.departmentIds().isEmpty() ? Expressions.FALSE
                        : root.getNumber(mapping.department(), Long.class).in(scope.departmentIds()));
        return root.getString(mapping.tenant()).eq(scope.tenant()).and(visible);
    }

    Specification<T> specification(DataPermissionQuery scope) {
        return (entity, criteria, cb) -> cb.and(cb.equal(entity.get(mapping.tenant()), scope.tenant()),
                scope.all() ? cb.conjunction() : cb.or(
                        scope.self() ? cb.equal(entity.get(mapping.owner()), ownerValue(scope)) : cb.disjunction(),
                        scope.departmentIds().isEmpty() ? cb.disjunction()
                                : entity.get(mapping.department()).in(scope.departmentIds())));
    }

    Predicate idPredicate(Object value) {
        return equality(id, DefaultConversionService.getSharedInstance().convert(value, idType));
    }

    Specification<T> idSpecification(Object value) {
        final var converted = DefaultConversionService.getSharedInstance().convert(value, idType);
        return (entity, criteria, cb) -> cb.equal(entity.get(id), converted);
    }

    private Object ownerValue(DataPermissionQuery scope) {
        return DefaultConversionService.getSharedInstance().convert(scope.userId(), ownerType);
    }

    private Predicate equality(String field, Object value) {
        return Expressions.booleanOperation(Ops.EQ, root.get(field, value.getClass()), Expressions.constant(value));
    }
}
