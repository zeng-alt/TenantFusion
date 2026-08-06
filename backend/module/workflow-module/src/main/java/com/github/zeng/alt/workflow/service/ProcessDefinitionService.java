package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.ProcessDefinitionVO;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.List;

/**
 * 流程定义服务接口
 *
 * @author zengAlt
 */
public interface ProcessDefinitionService {

    /**
     * 分页查询流程定义
     *
     * @param key           流程定义Key（可选）
     * @param name          流程定义名称（可选，模糊匹配）
     * @param suspended     是否挂起（可选）
     * @param latestVersion 是否仅查询最新版本
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 分页结果
     */
    PageRestResponse<ProcessDefinitionVO> queryDefinitions(String key, String name, Boolean suspended,
                                                           Boolean latestVersion, int pageNum, int pageSize);

    /**
     * 获取流程定义详情
     *
     * @param id 流程定义ID
     * @return 流程定义
     */
    ProcessDefinitionVO getDefinition(String id);

    /**
     * 部署流程定义
     *
     * @param name       部署名称
     * @param bpmnXml    BPMN XML内容
     * @param tenantId   租户ID（可选）
     * @return 部署的流程定义
     */
    ProcessDefinitionVO deploy(String name, String bpmnXml, String tenantId);

    /**
     * 删除流程定义（同时删除级联实例）
     *
     * @param id    流程定义ID
     * @param cascade 是否级联删除运行中的实例
     */
    void deleteDefinition(String id, boolean cascade);

    /**
     * 挂起流程定义
     *
     * @param id 流程定义ID
     */
    void suspendDefinition(String id);

    /**
     * 激活流程定义
     *
     * @param id 流程定义ID
     */
    void activateDefinition(String id);

    /**
     * 获取流程定义版本历史
     *
     * @param key 流程定义Key
     * @return 版本列表
     */
    List<ProcessDefinitionVO> getVersions(String key);

    /**
     * 获取流程定义的BPMN XML
     *
     * @param id 流程定义ID
     * @return BPMN XML字符串
     */
    String getBpmnXml(String id);

    /**
     * 将Camunda ProcessDefinition转换为VO
     */
    ProcessDefinitionVO toVO(ProcessDefinition pd);
}
