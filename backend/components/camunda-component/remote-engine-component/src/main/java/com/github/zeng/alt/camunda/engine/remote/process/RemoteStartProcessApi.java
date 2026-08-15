package com.github.zeng.alt.camunda.engine.remote.process;

import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartProcessApi;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.MessageApiClient;
import org.camunda.community.rest.client.api.ProcessDefinitionApiClient;
import org.camunda.community.rest.client.api.ProcessInstanceApiClient;
import org.camunda.community.rest.client.model.CorrelationMessageDto;
import org.camunda.community.rest.client.model.MessageCorrelationResultWithVariableDto;
import org.camunda.community.rest.client.model.ProcessInstanceDto;
import org.camunda.community.rest.client.model.ProcessInstanceModificationDto;
import org.camunda.community.rest.client.model.ProcessInstanceModificationInstructionDto;
import org.camunda.community.rest.client.model.ProcessInstanceWithVariablesDto;
import org.camunda.community.rest.client.model.StartProcessInstanceDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程流程启动实现（基于 Camunda 社区 REST 客户端）
 *
 * @author zengAlt
 */
@Service
public class RemoteStartProcessApi implements StartProcessApi {

    private final ProcessDefinitionApiClient processDefinitionApiClient;
    private final ProcessInstanceApiClient processInstanceApiClient;
    private final MessageApiClient messageApiClient;

    public RemoteStartProcessApi(ProcessDefinitionApiClient processDefinitionApiClient,
                                 ProcessInstanceApiClient processInstanceApiClient,
                                 MessageApiClient messageApiClient) {
        this.processDefinitionApiClient = processDefinitionApiClient;
        this.processInstanceApiClient = processInstanceApiClient;
        this.messageApiClient = messageApiClient;
    }

    @Override
    public ProcessInformation startByDefinition(StartByDefinitionCmd cmd) {
        StartProcessInstanceDto dto = new StartProcessInstanceDto();
        dto.setBusinessKey(cmd.getBusinessKey());
        dto.setVariables(RemoteSupport.toVariableMap(withInitiator(cmd.getVariables(), cmd.getInitiator())));
        ProcessInstanceWithVariablesDto pi;
        if (StringUtils.hasText(cmd.getTenantId())) {
            pi = processDefinitionApiClient
                    .startProcessInstanceByKeyAndTenantId(cmd.getProcessDefinitionKey(), cmd.getTenantId(), dto)
                    .getBody();
        } else {
            pi = processDefinitionApiClient.startProcessInstanceByKey(cmd.getProcessDefinitionKey(), dto).getBody();
        }
        return toProcessInformation(pi);
    }

    @Override
    public ProcessInformation startByMessage(StartByMessageCmd cmd) {
        CorrelationMessageDto dto = new CorrelationMessageDto();
        dto.setMessageName(cmd.getMessageName());
        dto.setBusinessKey(cmd.getBusinessKey());
        dto.setTenantId(cmd.getTenantId());
        dto.setProcessVariables(RemoteSupport.toVariableMap(withInitiator(cmd.getVariables(), cmd.getInitiator())));
        dto.setAll(false);
        dto.setResultEnabled(true);
        List<MessageCorrelationResultWithVariableDto> results = messageApiClient.deliverMessage(dto).getBody();
        ProcessInstanceDto pi = results.stream()
                .filter(r -> r.getProcessInstance() != null)
                .map(MessageCorrelationResultWithVariableDto::getProcessInstance)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("消息启动未产生流程实例: " + cmd.getMessageName()));
        return toProcessInformation(pi);
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
        startBeforeActivity(info.getInstanceId(), cmd.getElementId());
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
        startBeforeActivity(info.getInstanceId(), cmd.getElementId());
        return info;
    }

    private void startBeforeActivity(String processInstanceId, String elementId) {
        ProcessInstanceModificationInstructionDto instruction = new ProcessInstanceModificationInstructionDto();
        instruction.setType(ProcessInstanceModificationInstructionDto.TypeEnum.START_BEFORE_ACTIVITY);
        instruction.setActivityId(elementId);
        ProcessInstanceModificationDto modification = new ProcessInstanceModificationDto();
        modification.setInstructions(List.of(instruction));
        processInstanceApiClient.modifyProcessInstance(processInstanceId, modification);
    }

    private Map<String, Object> withInitiator(Map<String, Object> variables, String initiator) {
        Map<String, Object> map = variables == null ? new HashMap<>() : new HashMap<>(variables);
        if (StringUtils.hasText(initiator)) {
            map.put("initiator", initiator);
        }
        return map;
    }

    /**
     * 从流程定义ID（格式：key:version:generatedId）中提取流程定义Key
     */
    private String extractKey(String definitionId) {
        if (!StringUtils.hasText(definitionId)) {
            return null;
        }
        int idx = definitionId.indexOf(':');
        return idx > 0 ? definitionId.substring(0, idx) : definitionId;
    }

    private ProcessInformation toProcessInformation(ProcessInstanceWithVariablesDto pi) {
        return toProcessInformation(pi.getId(), pi.getDefinitionId(), pi.getBusinessKey(), pi.getTenantId());
    }

    private ProcessInformation toProcessInformation(ProcessInstanceDto pi) {
        return toProcessInformation(pi.getId(), pi.getDefinitionId(), pi.getBusinessKey(), pi.getTenantId());
    }

    private ProcessInformation toProcessInformation(String instanceId, String definitionId, String businessKey,
                                                    String tenantId) {
        if (instanceId == null) {
            throw new IllegalStateException("流程启动返回为空");
        }
        Map<String, String> meta = new HashMap<>();
        meta.put(ProcessInformation.META_PROCESS_DEFINITION_KEY, extractKey(definitionId));
        meta.put(ProcessInformation.META_PROCESS_DEFINITION_ID, definitionId);
        if (StringUtils.hasText(businessKey)) {
            meta.put(ProcessInformation.META_BUSINESS_KEY, businessKey);
        }
        if (StringUtils.hasText(tenantId)) {
            meta.put(ProcessInformation.META_TENANT_ID, tenantId);
        }
        meta.put(ProcessInformation.META_ROOT_PROCESS_INSTANCE_ID, instanceId);
        return ProcessInformation.builder().instanceId(instanceId).meta(meta).build();
    }
}
