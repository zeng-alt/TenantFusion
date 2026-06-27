package com.github.zeng.alt.admin.command.infrastructure.repository;

import com.github.zeng.alt.admin.command.infrastructure.entity.RolePermission;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface RolePermissionRepository extends BaseRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}