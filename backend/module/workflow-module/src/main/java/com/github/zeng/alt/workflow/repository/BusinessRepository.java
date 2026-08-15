package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.BusinessEntity;

import java.util.Optional;

/**
 * 业务 Repository
 *
 * @author zengAlt
 */
public interface BusinessRepository extends BaseRepository<BusinessEntity, Long> {

    boolean existsByCode(String code);

    /**
     * 按业务编码查询（编码全局唯一）
     *
     * @param code 业务编码
     * @return 业务
     */
    Optional<BusinessEntity> findByCode(String code);
}
