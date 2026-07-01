package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.UserResource;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface UserResourceRepository extends BaseRepository<UserResource, Long> {

    List<UserResource> findByUserId(Long userId);

    void deleteByUserIdAndResourceId(Long userId, Long resourceId);

    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);
}