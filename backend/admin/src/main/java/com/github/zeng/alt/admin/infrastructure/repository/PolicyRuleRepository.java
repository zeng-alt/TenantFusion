package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.List;

public interface PolicyRuleRepository extends BaseRepository<PolicyRule, Long> {

    List<PolicyRule> findByPermissionId(Long permissionId);
}