package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.WorkflowEntity;

import java.util.Optional;

/**
 * 流程主表 Repository
 *
 * @author zengAlt
 */
public interface WorkflowRepository extends BaseRepository<WorkflowEntity, Long> {

    /**
     * 判断流程编码是否已存在（用于创建时校验唯一性）
     *
     * @param workflowKey 流程编码
     * @return 是否已存在
     */
    boolean existsByWorkflowKey(String workflowKey);

    /**
     * 按流程编码查询流程
     *
     * @param workflowKey 流程编码
     * @return 流程
     */
    Optional<WorkflowEntity> findByWorkflowKey(String workflowKey);
}