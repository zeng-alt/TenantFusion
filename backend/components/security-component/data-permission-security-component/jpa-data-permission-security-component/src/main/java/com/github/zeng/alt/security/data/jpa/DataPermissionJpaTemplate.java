package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 持有受保护资源映射的查询门面，未注册实体不能执行授权查询。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionJpaTemplate {
    private final EntityManager entityManager;
    private final DataPermissionTemplate permissions;
    private final Map<Class<?>, DataPermissionMapping> mappings;

    public DataPermissionJpaTemplate(
            EntityManager entityManager,
            DataPermissionTemplate permissions,
            DataPermissionProperties properties
    ) {
        this.entityManager = entityManager;
        this.permissions = permissions;
        this.mappings = properties.getResources().entrySet().stream()
                .map(entry -> mapping(entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableMap(DataPermissionMapping::entity, mapping -> mapping));
    }

    public <T> DataPermissionJpaBuilder<T> query(Class<T> entity) {
        final var mapping = mappings.get(entity);
        if (mapping == null) {
            throw new IllegalArgumentException("Entity is not registered for data permissions: " + entity.getName());
        }
        return new DataPermissionJpaBuilder<>(entityManager, entity, permissions, mapping);
    }

    private DataPermissionMapping mapping(String name, DataPermissionProperties.Resource resource) {
        final var entity = entityManager.getMetamodel().entity(resource.getEntity());
        if (!entity.hasSingleIdAttribute()) {
            throw new IllegalArgumentException("Data permissions require a single entity ID: " + name);
        }
        requireType(entity.getSingularAttribute(resource.getTenant()), String.class);
        final var owner = entity.getSingularAttribute(resource.getOwner());
        if (owner.getJavaType() != Long.class && owner.getJavaType() != String.class) {
            throw new IllegalArgumentException("Owner field must be Long or String: " + name);
        }
        requireType(entity.getSingularAttribute(resource.getDepartment()), Long.class);
        return new DataPermissionMapping(name, resource.getEntity(), resource.getTenant(),
                resource.getOwner(), resource.getDepartment());
    }

    private void requireType(SingularAttribute<?, ?> attribute, Class<?> type) {
        if (attribute.getJavaType() != type) {
            throw new IllegalArgumentException("Invalid data permission field type: " + attribute.getName());
        }
    }
}
