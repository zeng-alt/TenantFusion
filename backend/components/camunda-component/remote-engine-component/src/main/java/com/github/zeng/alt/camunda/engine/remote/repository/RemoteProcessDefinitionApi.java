package com.github.zeng.alt.camunda.engine.remote.repository;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionInfo;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionQuery;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.DeploymentApiClient;
import org.camunda.community.rest.client.api.ProcessDefinitionApiClient;
import org.camunda.community.rest.client.model.CountResultDto;
import org.camunda.community.rest.client.model.ProcessDefinitionDto;
import org.camunda.community.rest.client.model.ProcessDefinitionSuspensionStateDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 远程流程定义仓库实现
 *
 * @author zengAlt
 */
@Service
public class RemoteProcessDefinitionApi implements ProcessDefinitionApi {

    private final ProcessDefinitionApiClient processDefinitionApiClient;
    private final DeploymentApiClient deploymentApiClient;

    public RemoteProcessDefinitionApi(ProcessDefinitionApiClient processDefinitionApiClient,
                                      DeploymentApiClient deploymentApiClient) {
        this.processDefinitionApiClient = processDefinitionApiClient;
        this.deploymentApiClient = deploymentApiClient;
    }

    @Override
    public PageRestResponse<ProcessDefinitionInfo> query(ProcessDefinitionQuery q) {
        long total = processDefinitionApiClient.getProcessDefinitionsCount(
                null, null, q.getName(), nameLike(q.getName()), null, null, null,
                q.getKey(), null, null, null, null, null, q.getLatestVersion(),
                null, null, null, active(q.getSuspended()), suspended(q.getSuspended()),
                null, null, null, null, null, null, null, null, null, null, null, null, null)
                .getBody().getCount();
        int firstResult = (q.getPageNo() - 1) * q.getPageSize();
        List<ProcessDefinitionDto> list = processDefinitionApiClient.getProcessDefinitions(
                null, null, q.getName(), nameLike(q.getName()), null, null, null,
                q.getKey(), null, null, null, null, null, q.getLatestVersion(),
                null, null, null, active(q.getSuspended()), suspended(q.getSuspended()),
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, firstResult, q.getPageSize()).getBody();
        List<ProcessDefinitionInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, q.getPageSize(), q.getPageNo());
    }

    @Override
    public ProcessDefinitionInfo get(String processDefinitionId) {
        ProcessDefinitionDto dto = processDefinitionApiClient.getProcessDefinition(processDefinitionId).getBody();
        if (dto == null) {
            throw new IllegalStateException("流程定义不存在: " + processDefinitionId);
        }
        return toInfo(dto);
    }

    @Override
    public ProcessDefinitionInfo getByDeploymentId(String deploymentId) {
        List<ProcessDefinitionDto> list = processDefinitionApiClient.getProcessDefinitions(
                null, null, null, null, deploymentId, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 1).getBody();
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("未找到流程定义: deploymentId=" + deploymentId);
        }
        return toInfo(list.get(0));
    }

    @Override
    public List<ProcessDefinitionInfo> versions(String processDefinitionKey) {
        List<ProcessDefinitionDto> list = processDefinitionApiClient.getProcessDefinitions(
                null, null, null, null, null, null, null,
                processDefinitionKey, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                "version", "desc", 0, 100).getBody();
        return list.stream().map(this::toInfo).toList();
    }

    @Override
    public byte[] getBpmnXml(String processDefinitionId) {
        String xml = processDefinitionApiClient
                .getProcessDefinitionBpmn20Xml(processDefinitionId).getBody().getBpmn20Xml();
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void suspend(String processDefinitionId) {
        ProcessDefinitionSuspensionStateDto dto = new ProcessDefinitionSuspensionStateDto();
        dto.setSuspended(true);
        processDefinitionApiClient.updateProcessDefinitionSuspensionStateById(processDefinitionId, dto);
    }

    @Override
    public void activate(String processDefinitionId) {
        ProcessDefinitionSuspensionStateDto dto = new ProcessDefinitionSuspensionStateDto();
        dto.setSuspended(false);
        processDefinitionApiClient.updateProcessDefinitionSuspensionStateById(processDefinitionId, dto);
    }

    @Override
    public void delete(String processDefinitionId, boolean cascade) {
        processDefinitionApiClient.deleteProcessDefinition(processDefinitionId, cascade, false, false);
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        deploymentApiClient.deleteDeployment(deploymentId, cascade, false, false);
    }

    private String nameLike(String name) {
        return StringUtils.hasText(name) ? "%" + name + "%" : null;
    }

    private Boolean active(Boolean suspended) {
        return suspended != null && !suspended;
    }

    private Boolean suspended(Boolean suspended) {
        return suspended != null && suspended;
    }

    private ProcessDefinitionInfo toInfo(ProcessDefinitionDto dto) {
        return ProcessDefinitionInfo.builder()
                .id(dto.getId())
                .key(dto.getKey())
                .name(dto.getName())
                .version(dto.getVersion())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .suspended(dto.getSuspended())
                .deploymentId(dto.getDeploymentId())
                .resourceName(dto.getResource())
                .diagramResourceName(dto.getDiagram())
                .tenantId(dto.getTenantId())
                .build();
    }
}
