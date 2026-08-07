package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormConfigEntity;

/**
 * 配置表单 Repository
 *
 * @author zengAlt
 */
public interface FormConfigRepository extends BaseRepository<FormConfigEntity, Long> {

    boolean existsByCode(String code);
}
