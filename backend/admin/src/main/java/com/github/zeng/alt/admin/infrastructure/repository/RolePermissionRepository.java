package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.RolePermission;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface RolePermissionRepository extends BaseRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    void deleteByRoleIdIn(Iterable<Long> ids);
}