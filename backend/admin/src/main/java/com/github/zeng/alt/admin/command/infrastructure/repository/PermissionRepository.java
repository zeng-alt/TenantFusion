package com.github.zeng.alt.admin.command.infrastructure.repository;

import com.github.zeng.alt.admin.command.infrastructure.entity.Permission;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface PermissionRepository extends BaseRepository<Permission, Long> {

    List<Permission> findByResourceType(String resourceType);
}