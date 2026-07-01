package com.github.zeng.alt.admin.infrastructure.repository;


import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends BaseRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findAllByOrderByRoleSortAsc();

    boolean existsByCode(String code);
}