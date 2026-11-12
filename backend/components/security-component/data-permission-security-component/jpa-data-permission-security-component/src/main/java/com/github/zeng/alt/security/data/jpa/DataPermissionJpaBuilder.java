package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.vavr.control.Either;
import io.vavr.control.Option;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.beans.Introspector;
import java.util.List;

/**
 * 单次查询构建器；终结操作重新授权，分页内容和计数共用一次授权快照。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionJpaBuilder<T> {
    private final EntityManager entityManager;
    private final DataPermissionTemplate permissions;
    private final DataPermissionMapping mapping;
    private final PathBuilder<T> root;
    private final DataPermissionPredicateFactory<T> predicates;
    private final SimpleJpaRepository<T, Object> repository;
    private final Querydsl querydsl;
    private Predicate business = Expressions.TRUE;
    private Specification<T> specification = (r, q, cb) -> cb.conjunction();
    private boolean criteriaMode;
    private boolean querydslMode;
    private String permission;

    DataPermissionJpaBuilder(
            EntityManager entityManager,
            Class<T> entity,
            DataPermissionTemplate permissions,
            DataPermissionMapping mapping
    ) {
        this.entityManager = entityManager;
        this.permissions = permissions;
        this.mapping = mapping;
        root = new PathBuilder<>(entity, Introspector.decapitalize(entity.getSimpleName()));
        predicates = new DataPermissionPredicateFactory<>(entityManager, mapping, root);
        repository = new SimpleJpaRepository<>(entity, entityManager);
        querydsl = new Querydsl(entityManager, root);
    }

    public DataPermissionJpaBuilder<T> permission(String permission) {
        this.permission = permission;
        return this;
    }

    public DataPermissionJpaBuilder<T> where(Predicate condition) {
        if (criteriaMode) {
            throw new IllegalStateException("Cannot mix Specification and Querydsl in one query");
        }
        querydslMode = true;
        business = new BooleanBuilder(business).and(condition);
        return this;
    }

    public DataPermissionJpaBuilder<T> where(Specification<T> condition) {
        if (querydslMode) {
            throw new IllegalStateException("Cannot mix Specification and Querydsl in one query");
        }
        criteriaMode = true;
        specification = specification.and(condition);
        return this;
    }

    public Either<String, Page<T>> page(Pageable pageable) {
        return scope().map(scope -> {
            if (criteriaMode) {
                return repository.findAll(specification.and(predicates.specification(scope)), pageable);
            }
            final List<T> content = querydsl.applyPagination(pageable, select(scope)).fetch();
            return new PageImpl<>(content, pageable, count(scope));
        });
    }

    public Either<String, List<T>> list(Sort sort) {
        return scope().map(scope -> criteriaMode
                ? repository.findAll(specification.and(predicates.specification(scope)), sort)
                : querydsl.applySorting(sort, select(scope)).fetch());
    }

    public Either<String, Option<T>> getDetail(Object id) {
        return scope().map(scope -> criteriaMode
                ? Option.ofOptional(repository.findOne(specification.and(predicates.specification(scope))
                        .and(predicates.idSpecification(id))))
                : Option.of(select(scope).where(predicates.idPredicate(id)).fetchFirst()));
    }

    public Either<String, Long> count() {
        return scope().map(this::count);
    }

    public Either<String, Boolean> exists() {
        return scope().map(scope -> criteriaMode
                ? repository.exists(specification.and(predicates.specification(scope)))
                : select(scope).select(Expressions.ONE).fetchFirst() != null);
    }

    private Either<String, DataPermissionQuery> scope() {
        if (permission == null || permission.isBlank()) {
            return Either.left("查询权限不能为空");
        }
        return permissions.resource(mapping.resource()).permission(permission).get();
    }

    private JPAQuery<T> select(DataPermissionQuery scope) {
        return new JPAQueryFactory(entityManager).selectFrom(root).where(business, predicates.querydsl(scope));
    }

    private long count(DataPermissionQuery scope) {
        if (criteriaMode) {
            return repository.count(specification.and(predicates.specification(scope)));
        }
        return Option.of(select(scope).select(root.count()).fetchOne()).getOrElse(0L);
    }
}
