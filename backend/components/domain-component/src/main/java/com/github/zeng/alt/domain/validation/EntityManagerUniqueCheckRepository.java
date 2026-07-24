package com.github.zeng.alt.domain.validation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 {@link EntityManager} 的默认 {@link IUniqueCheckRepository} 实现。
 * <p>
 * 通过 JPQL COUNT 查询检查指定字段值在数据库中是否唯一。当 JPA 可用时自动注入，
 * 用户可通过注册自定义 {@link IUniqueCheckRepository} 覆盖。
 *
 * @author zengJiaJun
 * @since 2026年07月23日
 */
public class EntityManagerUniqueCheckRepository implements IUniqueCheckRepository {

    private final EntityManager entityManager;

    public EntityManagerUniqueCheckRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isUnique(Class<?> entityClass, String field, Object value, Object id, boolean ignoreCase) {
        if (value == null) {
            return true;
        }

        StringBuilder jpql = new StringBuilder()
                .append("SELECT COUNT(e) FROM ")
                .append(entityClass.getName())
                .append(" e WHERE ");

        if (ignoreCase && value instanceof String) {
            jpql.append("LOWER(e.")
                    .append(field)
                    .append(") = LOWER(:value)");
        } else {
            jpql.append("e.")
                    .append(field)
                    .append(" = :value");
        }

        if (id != null) {
            jpql.append(" AND e.id <> :id");
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
        query.setParameter("value", value);
        if (id != null) {
            query.setParameter("id", id);
        }

        return query.getSingleResult() == 0;
    }
}
