package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormDataEntity;

import java.util.List;

/**
 * 动态表单数据 Repository
 *
 * @author zengAlt
 */
public interface FormDataRepository extends BaseRepository<FormDataEntity, Long> {

    /**
     * 按表单模板查询数据列表
     *
     * @param formTemplateId 表单模板ID
     * @return 数据列表
     */
    List<FormDataEntity> findByFormTemplateId(Long formTemplateId);

    /**
     * 按流程实例查询数据列表
     *
     * @param processInstanceId 流程实例ID
     * @return 数据列表
     */
    List<FormDataEntity> findByProcessInstanceId(String processInstanceId);
}
