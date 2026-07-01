package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // 列表查询已自动过滤 is_deleted = false, is_enabled = true, tenant_by = 当前租户
    List<User> findAllByOrderByCreatedDateDesc();
}