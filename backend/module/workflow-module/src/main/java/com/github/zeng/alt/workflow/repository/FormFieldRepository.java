package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormFieldEntity;

import java.util.List;

/**
 * 配置表单字段 Repository
 *
 * @author zengAlt
 */
public interface FormFieldRepository extends BaseRepository<FormFieldEntity, Long> {

    List<FormFieldEntity> findByVersionIdOrderBySortOrderAsc(Long versionId);

    List<FormFieldEntity> findByVersionIdAndParentFieldIdOrderBySortOrderAsc(Long versionId, Long parentFieldId);

    void deleteByVersionId(Long versionId);
}
