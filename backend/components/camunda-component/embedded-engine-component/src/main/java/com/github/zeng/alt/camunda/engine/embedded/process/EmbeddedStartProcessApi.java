package com.github.zeng.alt.camunda.engine.embedded.process;

import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartProcessApi;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 嵌入式流程启动实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedStartProcessApi implements StartProcessApi {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final IdentityService identityService;

    public EmbeddedStartProcessApi(RuntimeService runtimeService, RepositoryService repositoryService,
                                   IdentityService identityService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.identityService = identityService;
    }

    @Override
    public ProcessInformation startByDefinition(StartByDefinitionCmd cmd) {
        String initiator = cmd.getInitiator();
        return withInitiator(initiator, cmd.getTenantId(), () -> {
            Map<String, Object> variables = variables(cmd.getVariables());
            putInitiator(variables, initiator);
            ProcessInstance pi = startByKey(cmd.getProcessDefinitionKey(), cmd.getBusinessKey(),
                    cmd.getTenantId(), variables);
            return toProcessInformation(pi);
        });
    }

    @Override
    public ProcessInformation startByMessage(StartByMessageCmd cmd) {
        String initiator = cmd.getInitiator();
        return withInitiator(initiator, cmd.getTenantId(), () -> {
            Map<String, Object> variables = variables(cmd.getVariables());
            putInitiator(variables, initiator);
            MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(cmd.getMessageName());
            if (StringUtils.hasText(cmd.getBusinessKey())) {
                builder.processInstanceBusinessKey(cmd.getBusinessKey());
            }
            if (StringUtils.hasText(cmd.getTenantId())) {
                builder.tenantId(cmd.getTenantId());
            }
            ProcessInstance pi = builder.setVariables(variables).correlateStartMessage();
            return toProcessInformation(pi);
        });
    }

    private <T> T withInitiator(String initiator, String tenant, Supplier<T> action) {
        if (!StringUtils.hasText(initiator)) {
            return action.get();
        }
        final Authentication previous = identityService.getCurrentAuthentication();
        if (previous == null) {
            identityService.setAuthentication(initiator, List.of(), StringUtils.hasText(tenant) ? List.of(tenant) : List.of());
        } else {
            identityService.setAuthentication(initiator, previous.getGroupIds(), previous.getTenantIds());
        }
        try {
            return action.get();
        } finally {
            identityService.setAuthentication(previous);
        }
    }

    @Override
    public ProcessInformation startByDefinitionAtElement(StartByDefinitionAtElementCmd cmd) {
        ProcessInformation info = startByDefinition(StartByDefinitionCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(cmd.getInitiator())
                .tenantId(cmd.getTenantId())
                .build());
        startBeforeActivity(info, cmd.getElementId());
        return info;
    }

    @Override
    public ProcessInformation startByMessageAtElement(StartByMessageAtElementCmd cmd) {
        ProcessInformation info = startByMessage(StartByMessageCmd.builder()
                .messageName(cmd.getMessageName())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(cmd.getInitiator())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(cmd.getTenantId())
                .build());
        startBeforeActivity(info, cmd.getElementId());
        return info;
    }

    private void startBeforeActivity(ProcessInformation info, String elementId) {
        String processDefinitionId = info.getMeta().get(ProcessInformation.META_PROCESS_DEFINITION_ID);
        runtimeService.createModification(processDefinitionId)
                .processInstanceIds(info.getInstanceId())
                .startBeforeActivity(elementId)
                .execute();
    }

    private ProcessInstance startByKey(String processDefinitionKey, String businessKey, String tenantId,
                                       Map<String, Object> variables) {
        if (StringUtils.hasText(tenantId)) {
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .tenantIdIn(tenantId)
                    .active()
                    .latestVersion()
                    .singleResult();
            if (pd == null) {
                throw new IllegalStateException("未找到流程定义: " + processDefinitionKey + " (tenant=" + tenantId + ")");
            }
            return runtimeService.startProcessInstanceById(pd.getId(), businessKey, variables);
        }
        if (StringUtils.hasText(businessKey)) {
            return runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
        }
        return runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    private Map<String, Object> variables(Map<String, Object> variables) {
        return variables == null ? new HashMap<>() : new HashMap<>(variables);
    }

    private void putInitiator(Map<String, Object> variables, String initiator) {
        if (StringUtils.hasText(initiator)) {
            variables.put("initiator", initiator);
        }
    }

    private ProcessInformation toProcessInformation(ProcessInstance pi) {
        Map<String, String> meta = new HashMap<>();
        meta.put(ProcessInformation.META_PROCESS_DEFINITION_KEY, pi.getProcessDefinitionKey());
        meta.put(ProcessInformation.META_PROCESS_DEFINITION_ID, pi.getProcessDefinitionId());
        if (StringUtils.hasText(pi.getBusinessKey())) {
            meta.put(ProcessInformation.META_BUSINESS_KEY, pi.getBusinessKey());
        }
        if (StringUtils.hasText(pi.getTenantId())) {
            meta.put(ProcessInformation.META_TENANT_ID, pi.getTenantId());
        }
        if (StringUtils.hasText(pi.getRootProcessInstanceId())) {
            meta.put(ProcessInformation.META_ROOT_PROCESS_INSTANCE_ID, pi.getRootProcessInstanceId());
        }
        return ProcessInformation.builder().instanceId(pi.getId()).meta(meta).build();
    }
}
