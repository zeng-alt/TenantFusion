package com.github.zeng.alt.camunda.engine.remote.process;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceApi;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceQuery;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.ProcessInstanceApiClient;
import org.camunda.community.rest.client.model.ProcessInstanceDto;
import org.camunda.community.rest.client.model.ProcessInstanceQueryDto;
import org.camunda.community.rest.client.model.SuspensionStateDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 远程流程实例运行时实现
 *
 * @author zengAlt
 */
@Service
public class RemoteProcessInstanceApi implements ProcessInstanceApi {

    private final ProcessInstanceApiClient processInstanceApiClient;

    public RemoteProcessInstanceApi(ProcessInstanceApiClient processInstanceApiClient) {
        this.processInstanceApiClient = processInstanceApiClient;
    }

    @Override
    public PageRestResponse<ProcessInstanceInfo> query(ProcessInstanceQuery q) {
        ProcessInstanceQueryDto dto = new ProcessInstanceQueryDto();
        dto.setProcessDefinitionKey(q.getProcessDefinitionKey());
        dto.setBusinessKey(q.getBusinessKey());
        if (q.getSuspended() != null && q.getSuspended()) {
            dto.setSuspended(true);
        } else if (q.getSuspended() != null) {
            dto.setActive(true);
        }
        if (StringUtils.hasText(q.getTenantId())) {
            dto.setTenantIdIn(List.of(q.getTenantId()));
        }
        dto.setSorting(List.of());
        long total = processInstanceApiClient.queryProcessInstancesCount(dto).getBody().getCount();
        int firstResult = (q.getPageNo() - 1) * q.getPageSize();
        List<ProcessInstanceDto> list = processInstanceApiClient
                .queryProcessInstances(firstResult, q.getPageSize(), dto).getBody();
        List<ProcessInstanceInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, q.getPageSize(), q.getPageNo());
    }

    @Override
    public ProcessInstanceInfo get(String processInstanceId) {
        ProcessInstanceDto dto = processInstanceApiClient.getProcessInstance(processInstanceId).getBody();
        if (dto == null) {
            throw new IllegalStateException("流程实例不存在: " + processInstanceId);
        }
        return toInfo(dto);
    }

    @Override
    public void suspend(String processInstanceId) {
        SuspensionStateDto dto = new SuspensionStateDto();
        dto.setSuspended(true);
        processInstanceApiClient.updateSuspensionStateById(processInstanceId, dto);
    }

    @Override
    public void activate(String processInstanceId) {
        SuspensionStateDto dto = new SuspensionStateDto();
        dto.setSuspended(false);
        processInstanceApiClient.updateSuspensionStateById(processInstanceId, dto);
    }

    @Override
    public void delete(String processInstanceId, String reason) {
        processInstanceApiClient.deleteProcessInstance(processInstanceId, false, false, false, true);
    }

    @Override
    public Map<String, Object> getVariables(String processInstanceId) {
        return RemoteSupport.fromVariableMap(
                processInstanceApiClient.getProcessInstanceVariables(processInstanceId, false).getBody());
    }

    @Override
    public void setVariables(String processInstanceId, Map<String, Object> variables) {
        if (variables != null) {
            variables.forEach((name, value) -> processInstanceApiClient.setProcessInstanceVariable(
                    processInstanceId, name, RemoteSupport.variableValue(value)));
        }
    }

    private ProcessInstanceInfo toInfo(ProcessInstanceDto dto) {
        return ProcessInstanceInfo.builder()
                .id(dto.getId())
                .businessKey(dto.getBusinessKey())
                .processDefinitionId(dto.getDefinitionId())
                .processDefinitionKey(dto.getDefinitionKey())
                .suspended(dto.getSuspended())
                .ended(dto.getEnded())
                .tenantId(dto.getTenantId())
                .build();
    }
}
