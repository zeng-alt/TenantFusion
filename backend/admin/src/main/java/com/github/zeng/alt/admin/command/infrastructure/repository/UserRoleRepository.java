package com.github.zeng.alt.admin.command.infrastructure.repository;

import com.github.zeng.alt.admin.command.infrastructure.entity.UserRole;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface UserRoleRepository extends BaseRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}



