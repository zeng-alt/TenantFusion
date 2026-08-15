package com.github.zeng.alt.camunda.engine.embedded.repository;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionInfo;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 嵌入式流程定义仓库实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedProcessDefinitionApi implements ProcessDefinitionApi {

    private final RepositoryService repositoryService;

    public EmbeddedProcessDefinitionApi(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public PageRestResponse<ProcessDefinitionInfo> query(ProcessDefinitionQuery q) {
        org.camunda.bpm.engine.repository.ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (StringUtils.hasText(q.getKey())) {
            query.processDefinitionKey(q.getKey());
        }
        if (StringUtils.hasText(q.getName())) {
            query.processDefinitionNameLike("%" + q.getName() + "%");
        }
        if (q.getSuspended() != null && q.getSuspended()) {
            query.suspended();
        } else if (q.getSuspended() != null && !q.getSuspended()) {
            query.active();
        }
        if (q.getLatestVersion() != null && q.getLatestVersion()) {
            query.latestVersion();
        }
        if (StringUtils.hasText(q.getTenantId())) {
            query.tenantIdIn(q.getTenantId());
        }
        query.orderByProcessDefinitionKey().asc().orderByProcessDefinitionVersion().desc();

        long total = query.count();
        int firstResult = (q.getPageNo() - 1) * q.getPageSize();
        List<ProcessDefinition> list = query.listPage(firstResult, q.getPageSize());
        List<ProcessDefinitionInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, q.getPageSize(), q.getPageNo());
    }

    @Override
    public ProcessDefinitionInfo get(String processDefinitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (pd == null) {
            throw new IllegalStateException("流程定义不存在: " + processDefinitionId);
        }
        return toInfo(pd);
    }

    @Override
    public ProcessDefinitionInfo getByDeploymentId(String deploymentId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        if (pd == null) {
            throw new IllegalStateException("未找到流程定义: deploymentId=" + deploymentId);
        }
        return toInfo(pd);
    }

    @Override
    public List<ProcessDefinitionInfo> versions(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc()
                .list().stream().map(this::toInfo).toList();
    }

    @Override
    public byte[] getBpmnXml(String processDefinitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (pd == null) {
            throw new IllegalStateException("流程定义不存在: " + processDefinitionId);
        }
        try (var is = repositoryService.getProcessModel(processDefinitionId)) {
            return is.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取BPMN XML失败: " + processDefinitionId, e);
        }
    }

    @Override
    public void suspend(String processDefinitionId) {
        repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
    }

    @Override
    public void activate(String processDefinitionId) {
        repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
    }

    @Override
    public void delete(String processDefinitionId, boolean cascade) {
        repositoryService.deleteProcessDefinition(processDefinitionId, cascade);
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    private ProcessDefinitionInfo toInfo(ProcessDefinition pd) {
        return ProcessDefinitionInfo.builder()
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
