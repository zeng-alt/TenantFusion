package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartProcessApi;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.workflow.service.WorkflowStarterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

/**
 * 流程启动服务实现
 * <p>
 * 统一走 {@link StartProcessApi}，发起人由各引擎实现负责记录。
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class WorkflowStarterServiceImpl implements WorkflowStarterService {

    private final StartProcessApi startProcessApi;

    @Override
    public ProcessInformation startByProcessDefinition(
            com.github.zeng.alt.workflow.model.StartByProcessDefinitionCmd cmd) {
        return startProcessApi.startByDefinition(StartByDefinitionCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
    }

    @Override
    public ProcessInformation startByMessage(com.github.zeng.alt.workflow.model.StartByMessageCmd cmd) {
        return startProcessApi.startByMessage(StartByMessageCmd.builder()
                .messageName(cmd.getMessageName())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
    }

    @Override
    public ProcessInformation startByDefinitionAtActivity(
            com.github.zeng.alt.workflow.model.StartByDefinitionAtElementCmd cmd) {
        return startProcessApi.startByDefinitionAtElement(StartByDefinitionAtElementCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .elementId(cmd.getElementId())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
    }

    @Override
    public ProcessInformation startByMessageAtActivity(
            com.github.zeng.alt.workflow.model.StartByMessageAtElementCmd cmd) {
        return startProcessApi.startByMessageAtElement(StartByMessageAtElementCmd.builder()
                .messageName(cmd.getMessageName())
                .elementId(cmd.getElementId())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
    }
}
