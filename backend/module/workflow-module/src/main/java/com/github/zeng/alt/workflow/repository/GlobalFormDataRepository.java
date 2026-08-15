package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.GlobalFormDataEntity;

import java.util.List;
import java.util.Optional;

/**
 * 流程全局表单数据 Repository
 *
 * @author zengAlt
 */
public interface GlobalFormDataRepository extends BaseRepository<GlobalFormDataEntity, Long> {

    /**
     * 按流程实例ID查询数据列表
     *
     * @param processInstanceId 流程实例ID
     * @return 数据列表
     */
    List<GlobalFormDataEntity> findByProcessInstanceId(String processInstanceId);

    /**
     * 按流程实例ID查询最近一次提交的数据
     *
     * @param processInstanceId 流程实例ID
     * @return 最近提交的数据
     */
    Optional<GlobalFormDataEntity> findFirstByProcessInstanceIdOrderByLastModifiedDateDesc(String processInstanceId);
}
