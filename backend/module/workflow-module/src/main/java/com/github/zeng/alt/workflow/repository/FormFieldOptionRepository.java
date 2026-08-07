package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormFieldOptionEntity;

import java.util.List;

/**
 * 配置表单字段选项 Repository
 *
 * @author zengAlt
 */
public interface FormFieldOptionRepository extends BaseRepository<FormFieldOptionEntity, Long> {

    List<FormFieldOptionEntity> findByFieldIdOrderBySortOrderAsc(Long fieldId);

    void deleteByFieldId(Long fieldId);
}
