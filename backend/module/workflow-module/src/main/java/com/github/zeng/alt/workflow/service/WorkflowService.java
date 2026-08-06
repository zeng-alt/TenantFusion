package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.WorkflowCreateCmd;
import com.github.zeng.alt.workflow.model.WorkflowQuery;
import com.github.zeng.alt.workflow.model.WorkflowSaveDraftCmd;
import com.github.zeng.alt.workflow.model.WorkflowUpdateCmd;
import com.github.zeng.alt.workflow.model.WorkflowVO;
import com.github.zeng.alt.workflow.model.WorkflowVersionVO;

import java.io.IOException;
import java.util.List;

/**
 * 流程管理服务接口
 * <p>
 * 提供流程主数据 CRUD、草稿保存、版本发布、下线、挂起/激活等能力。
 *
 * @author zengAlt
 */
public interface WorkflowService {

    /**
     * 分页查询流程
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageRestResponse<WorkflowVO> page(WorkflowQuery query);

    /**
     * 获取流程详情
     *
     * @param id 流程ID
     * @return 流程
     */
    WorkflowVO getDetail(Long id);

    /**
     * 创建流程（自动创建 1.0 草稿版本）
     *
     * @param cmd 创建命令
     * @return 创建的流程
     */
    WorkflowVO create(WorkflowCreateCmd cmd);

    /**
     * 更新流程主数据
     *
     * @param id  流程ID
     * @param cmd 更新命令
     * @return 更新后的流程
     */
    WorkflowVO update(Long id, WorkflowUpdateCmd cmd);

    /**
     * 删除流程（同时删除全部版本；若存在已发布版本且被引用则抛异常）
     *
     * @param id 流程ID
     */
    void delete(Long id);

    /**
     * 查询流程的全部版本
     *
     * @param workflowId 流程ID
     * @return 版本列表
     */
    List<WorkflowVersionVO> versions(Long workflowId);

    /**
     * 获取版本详情（含 BPMN XML）
     *
     * @param versionId 版本ID
     * @return 版本
     */
    WorkflowVersionVO getVersion(Long versionId) throws IOException;

    /**
     * 获取版本详情（不含 BPMN XML）
     *
     * @param templateId 模板Id
     * @param version 版本
     * @return 版本
     */
    WorkflowVersionVO getVersion(Long templateId, Integer version);

    /**
     * 保存草稿：若存在草稿版本则更新，否则创建新版本
     *
     * @param workflowId 流程ID
     * @param cmd        保存命令
     * @return 草稿版本
     */
    WorkflowVersionVO saveDraft(Long workflowId, WorkflowSaveDraftCmd cmd);

    /**
     * 保存并发布：创建新版本
     *
     * @param workflowId 流程ID
     * @param cmd        保存命令
     * @return 草稿版本
     */
    WorkflowVersionVO saveDraftAndPublish(Long workflowId, WorkflowSaveDraftCmd cmd);

    /**
     * 上线流程版本（草稿或已下线 → 已发布）：校验 XML 后部署到 Camunda，置为已发布并成为当前生效版本
     *
     * @param versionId 版本ID
     * @return 上线后的版本
     */
    WorkflowVersionVO publish(Long versionId);

    /**
     * 下线版本：状态置为已下线，若为当前版本则同步挂起 Camunda 流程定义
     *
     * @param versionId 版本ID
     * @return 下线后的版本
     */
    WorkflowVersionVO offline(Long versionId);
}
