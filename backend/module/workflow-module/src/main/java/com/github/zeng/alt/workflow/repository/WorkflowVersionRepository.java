package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.WorkflowVersionEntity;
import com.github.zeng.alt.workflow.model.WorkflowVersionStatus;
import io.vavr.control.Option;

import java.util.List;
import java.util.Optional;

/**
 * 流程版本表 Repository
 *
 * @author zengAlt
 */
public interface WorkflowVersionRepository extends BaseRepository<WorkflowVersionEntity, Long> {


    Option<WorkflowVersionListProjection> findByWorkflowIdAndVersion(Long workflowId, Integer version);

    /**
     * 查询流程的全部版本，按版本号倒序
     *
     * @param workflowId 流程ID
     * @return 版本列表
     */
    List<WorkflowVersionEntity> findByWorkflowIdOrderByVersionDesc(Long workflowId);

    List<WorkflowVersionEntity> findProjectByWorkflowIdOrderByVersionDesc(Long workflowId);

    /**
     * 查询流程中草稿版本（最多一条）
     *
     * @param workflowId 流程ID
     * @param status     状态
     * @return 版本记录
     */
    Optional<WorkflowVersionEntity> findFirstByWorkflowIdAndStatusOrderByVersionDesc(Long workflowId, WorkflowVersionStatus status);

    /**
     * 查询流程当前生效版本
     *
     * @param workflowId 流程ID
     * @return 版本记录
     */
    Optional<WorkflowVersionEntity> findFirstByWorkflowIdAndCurrentTrue(Long workflowId);

    /**
     * 按流程定义ID查询版本记录（用于按 processDefinitionId 回溯 BPMN XML）
     *
     * @param processDefinitionId 流程定义ID
     * @return 版本记录
     */
    Optional<WorkflowVersionEntity> findFirstByProcessDefinitionId(String processDefinitionId);

    /**
     * 查询流程中指定状态的版本列表
     *
     * @param workflowId 流程ID
     * @param status     状态
     * @return 版本列表
     */
    List<WorkflowVersionEntity> findByWorkflowIdAndStatus(Long workflowId, WorkflowVersionStatus status);

    /**
     * 删除流程下的全部版本
     *
     * @param workflowId 流程ID
     */
    void deleteByWorkflowId(Long workflowId);
}