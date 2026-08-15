package com.github.zeng.alt.camunda.engine.api.repository;

import com.github.zeng.alt.api.rest.PageRestResponse;

import java.util.List;

/**
 * 流程定义仓库 API
 * <p>
 * 对应嵌入式 RepositoryService 的流程定义查询/管理能力。
 *
 * @author zengAlt
 */
public interface ProcessDefinitionApi {

    /**
     * 分页查询流程定义
     */
    PageRestResponse<ProcessDefinitionInfo> query(ProcessDefinitionQuery query);

    /**
     * 查询流程定义详情
     */
    ProcessDefinitionInfo get(String processDefinitionId);

    /**
     * 按部署ID查询流程定义
     */
    ProcessDefinitionInfo getByDeploymentId(String deploymentId);

    /**
     * 查询流程定义的所有版本（按版本号倒序）
     */
    List<ProcessDefinitionInfo> versions(String processDefinitionKey);

    /**
     * 获取流程定义 BPMN XML
     */
    byte[] getBpmnXml(String processDefinitionId);

    /**
     * 挂起流程定义（影响后续实例）
     */
    void suspend(String processDefinitionId);

    /**
     * 激活流程定义
     */
    void activate(String processDefinitionId);

    /**
     * 删除流程定义
     */
    void delete(String processDefinitionId, boolean cascade);

    /**
     * 删除部署
     */
    void deleteDeployment(String deploymentId, boolean cascade);
}
