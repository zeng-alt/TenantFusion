package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在分页之后批量预加载角色，避免集合 fetch join 使数据库分页退化为内存分页。
 * 显式 fetch 角色也避免原生环境生成 Hibernate 懒代理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class UserQueryRoleResolver {
    private final EntityManager entityManager;

    public List<User> resolve(String tenant, List<User> users) {
        if (users.isEmpty()) {
            return users;
        }
        entityManager.createQuery("""
                select relation from UserRole relation join fetch relation.role role
                where relation.tenantBy = :tenant and relation.user.tenantBy = :tenant
                  and role.tenantBy = :tenant and relation.user.userId in :ids
                """, UserRole.class).setParameter("tenant", tenant)
                .setParameter("ids", users.stream().map(User::getUserId).toList()).getResultList();
        users.forEach(user -> user.getUserRoles().size());
        return users;
    }
}
