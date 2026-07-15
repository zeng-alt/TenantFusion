package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.domain.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface PermissionRepository extends BaseRepository<Permission, Long> {

    Stream<Permission> findAllByPermissionIdIn(Collection<Long> id);

    /**
     * superAdmin：查询所有 Permission
     */
    @Query("""
        select p from Permission p
    """)
    List<Permission> findAllPermissions();

    @Query("""
        select p from Permission p where p.enabled = true or p.enabled is null
    """)
    List<Permission> findAllEnablePermissions();

    /**
     * 普通用户：根据 roleCode 查询 Permission
     */
    @Query("""
        select rp.permission
        from RolePermission rp
        join rp.role r
        where r.code = :roleCode
    """)
    List<Permission> findPermissionsByRoleCode(@Param("roleCode") String roleCode);
}