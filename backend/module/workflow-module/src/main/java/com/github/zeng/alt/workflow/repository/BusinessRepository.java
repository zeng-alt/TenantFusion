package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.BusinessEntity;

/**
 * 业务 Repository
 *
 * @author zengAlt
 */
public interface BusinessRepository extends BaseRepository<BusinessEntity, Long> {

    boolean existsByCode(String code);
}
