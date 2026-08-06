package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.ProcessDefinitionVO;
import com.github.zeng.alt.workflow.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 流程定义服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final RepositoryService repositoryService;

    @Override
    public PageRestResponse<ProcessDefinitionVO> queryDefinitions(String key, String name, Boolean suspended,
                                                                   Boolean latestVersion, int pageNum, int pageSize) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

        if (key != null && !key.isBlank()) {
            query.processDefinitionKey(key);
        }
        if (name != null && !name.isBlank()) {
            query.processDefinitionNameLike("%" + name + "%");
        }
        if (suspended != null && suspended) {
            query.suspended();
        } else if (suspended != null && !suspended) {
            query.active();
        }
        if (latestVersion != null && latestVersion) {
            query.latestVersion();
        }

        query.orderByProcessDefinitionKey().asc()
                .orderByProcessDefinitionVersion().desc();

        long total = query.count();
        List<ProcessDefinition> list = query.listPage((pageNum - 1) * pageSize, pageSize);
        List<ProcessDefinitionVO> vos = list.stream().map(this::toVO).toList();

        return PageRestResponse.of(vos, total, pageSize, pageNum);
    }

    @Override
    public ProcessDefinitionVO getDefinition(String id) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id)
                .singleResult();
        if (pd == null) {
            throw new RuntimeException("流程定义不存在: " + id);
        }
        return toVO(pd);
    }

    @Override
    public ProcessDefinitionVO deploy(String name, String bpmnXml, String tenantId) {
        DeploymentBuilder builder = repositoryService.createDeployment()
                .name(name)
                .addString("process.bpmn", bpmnXml);
        if (tenantId != null && !tenantId.isBlank()) {
            builder.tenantId(tenantId);
        }
        Deployment deployment = builder.deploy();

        // 获取部署的流程定义
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (pd == null) {
            throw new RuntimeException("部署成功但未找到流程定义");
        }
        log.info("部署流程定义成功: " + pd.getKey() + " v" + pd.getVersion());
        return toVO(pd);
    }

    @Override
    public void deleteDefinition(String id, boolean cascade) {
        repositoryService.deleteProcessDefinition(id, cascade);
        log.info("删除流程定义: " + id + ", 级联: " + cascade);
    }

    @Override
    public void suspendDefinition(String id) {
        repositoryService.suspendProcessDefinitionById(id, true, null);
        log.info("挂起流程定义: " + id);
    }

    @Override
    public void activateDefinition(String id) {
        repositoryService.activateProcessDefinitionById(id, true, null);
        log.info("激活流程定义: " + id);
    }

    @Override
    public List<ProcessDefinitionVO> getVersions(String key) {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .orderByProcessDefinitionVersion().desc()
                .list();
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public String getBpmnXml(String id) {
        ProcessDefinition pd = repositoryService.getProcessDefinition(id);
        if (pd == null) {
            throw new RuntimeException("流程定义不存在: " + id);
        }
        try {
            byte[] bytes = repositoryService.getProcessModel(id).readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取BPMN XML失败: " + id, e);
        }
    }

    @Override
    public ProcessDefinitionVO toVO(ProcessDefinition pd) {
        return ProcessDefinitionVO.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(pd.getName())
                .version(pd.getVersion())
                .description(pd.getDescription())
                .category(pd.getCategory())
                .suspended(pd.isSuspended())
                .deploymentId(pd.getDeploymentId())
                .resourceName(pd.getResourceName())
                .diagramResourceName(pd.getDiagramResourceName())
                .tenantId(pd.getTenantId())
                .build();
    }
}
