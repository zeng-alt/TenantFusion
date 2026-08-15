package com.github.zeng.alt.camunda.engine.embedded.process;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceApi;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceQuery;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 嵌入式流程实例运行时实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedProcessInstanceApi implements ProcessInstanceApi {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;

    public EmbeddedProcessInstanceApi(RuntimeService runtimeService, RepositoryService repositoryService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
    }

    @Override
    public PageRestResponse<ProcessInstanceInfo> query(ProcessInstanceQuery q) {
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery camundaQuery = runtimeService.createProcessInstanceQuery();
        if (StringUtils.hasText(q.getProcessDefinitionKey())) {
            camundaQuery.processDefinitionKey(q.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(q.getBusinessKey())) {
            camundaQuery.processInstanceBusinessKey(q.getBusinessKey());
        }
        if (q.getSuspended() != null && q.getSuspended()) {
            camundaQuery.suspended();
        } else if (q.getSuspended() != null && !q.getSuspended()) {
            camundaQuery.active();
        }
        if (StringUtils.hasText(q.getTenantId())) {
            camundaQuery.tenantIdIn(q.getTenantId());
        }
        camundaQuery.orderByProcessInstanceId().desc();

        long total = camundaQuery.count();
        int firstResult = (q.getPageNo() - 1) * q.getPageSize();
        List<ProcessInstance> list = camundaQuery.listPage(firstResult, q.getPageSize());
        List<ProcessInstanceInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, q.getPageSize(), q.getPageNo());
    }

    @Override
    public ProcessInstanceInfo get(String processInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (pi == null) {
            throw new IllegalStateException("流程实例不存在: " + processInstanceId);
        }
        return toInfo(pi);
    }

    @Override
    public void suspend(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    @Override
    public void activate(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    @Override
    public void delete(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    @Override
    public Map<String, Object> getVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    @Override
    public void setVariables(String processInstanceId, Map<String, Object> variables) {
        runtimeService.setVariables(processInstanceId, variables);
    }

    private ProcessInstanceInfo toInfo(ProcessInstance pi) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(pi.getProcessDefinitionId())
                .singleResult();
        ProcessInstanceInfo.ProcessInstanceInfoBuilder builder = ProcessInstanceInfo.builder()
                .id(pi.getId())
                .businessKey(pi.getBusinessKey())
                .processDefinitionId(pi.getProcessDefinitionId())
                .suspended(pi.isSuspended())
                .ended(pi.isEnded())
                .tenantId(pi.getTenantId());
        if (pd != null) {
            builder.processDefinitionKey(pd.getKey())
                    .processDefinitionName(pd.getName())
                    .processDefinitionVersion(pd.getVersion());
        }
        return builder.build();
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
