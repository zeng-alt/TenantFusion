package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.domain.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRuleRepository extends BaseRepository<PolicyRule, Long> {

    @Query("""
        select p from PolicyRule p
        where p.permission.permissionId = :permissionId
        """)
    List<PolicyRule> findByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 按租户及启用的权限读取全部规则，保留资源范围与授权阶段供 ABAC 组件判定。
     * join fetch 权限关联：映射器要读 permission.code，而授权发生在无 OSIV 的过滤器链中。
     */
    @Query("""
        select p from PolicyRule p
        join fetch p.permission perm
        where p.tenantBy = :tenant
          and perm.tenantBy = :tenant
          and perm.code = :permission
          and perm.enabled = true
        order by p.policyRuleId
        """)
    List<PolicyRule> listByPermission(@Param("tenant") String tenant, @Param("permission") String permission);
}