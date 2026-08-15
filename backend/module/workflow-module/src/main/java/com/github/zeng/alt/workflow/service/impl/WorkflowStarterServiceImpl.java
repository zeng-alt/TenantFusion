package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageAtElementCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartByMessageCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartProcessApi;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;
import com.github.zeng.alt.workflow.model.GlobalFormType;
import com.github.zeng.alt.workflow.service.GlobalFormDataService;
import com.github.zeng.alt.workflow.service.GlobalFormDefinitionService;
import com.github.zeng.alt.workflow.service.WorkflowStarterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 流程启动服务实现
 * <p>
 * 统一走 {@link StartProcessApi}，发起人由各引擎实现负责记录。
 * 发起后若流程配置了全局表单，则初始化一条全局表单数据（含表单定义快照）。
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class WorkflowStarterServiceImpl implements WorkflowStarterService {

    private final StartProcessApi startProcessApi;
    private final GlobalFormDataService globalFormDataService;
    private final GlobalFormDefinitionService globalFormDefinitionService;

    @Override
    public ProcessInformation startByProcessDefinition(
            com.github.zeng.alt.workflow.model.StartByProcessDefinitionCmd cmd) {
        ProcessInformation info = startProcessApi.startByDefinition(StartByDefinitionCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
        initGlobalFormData(info);
        return info;
    }

    @Override
    public ProcessInformation startByMessage(com.github.zeng.alt.workflow.model.StartByMessageCmd cmd) {
        ProcessInformation info = startProcessApi.startByMessage(StartByMessageCmd.builder()
                .messageName(cmd.getMessageName())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
        initGlobalFormData(info);
        return info;
    }

    @Override
    public ProcessInformation startByDefinitionAtActivity(
            com.github.zeng.alt.workflow.model.StartByDefinitionAtElementCmd cmd) {
        ProcessInformation info = startProcessApi.startByDefinitionAtElement(StartByDefinitionAtElementCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .elementId(cmd.getElementId())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
        initGlobalFormData(info);
        return info;
    }

    @Override
    public ProcessInformation startByMessageAtActivity(
            com.github.zeng.alt.workflow.model.StartByMessageAtElementCmd cmd) {
        ProcessInformation info = startProcessApi.startByMessageAtElement(StartByMessageAtElementCmd.builder()
                .messageName(cmd.getMessageName())
                .elementId(cmd.getElementId())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(UserContextHolder.getUsername())
                .processDefinitionId(cmd.getProcessDefinitionId())
                .tenantId(TenantContextHolder.getTenantId())
                .build());
        initGlobalFormData(info);
        return info;
    }

    /**
     * 发起后初始化全局表单数据：流程配置了全局表单时才创建。
     * CAMUNDA 类型不保存 FormKit 定义（每次实时解析最新版本），EXTERNAL/GENERATED 保存其定义。
     */
    private void initGlobalFormData(ProcessInformation info) {
        if (info == null || !StringUtils.hasText(info.getInstanceId())) {
            return;
        }
        String workflowCode = info.getMeta() == null ? null
                : info.getMeta().get(ProcessInformation.META_PROCESS_DEFINITION_KEY);
        if (!StringUtils.hasText(workflowCode)) {
            return;
        }
        try {
            GlobalFormDefinitionVO definition = globalFormDefinitionService.resolveByWorkflowCode(workflowCode);
            if (definition == null) {
                return;
            }
            if (definition.getType() == GlobalFormType.CAMUNDA) {
                definition.setDefinition(null);
            }
            globalFormDataService.initialize(info.getInstanceId(), workflowCode, definition);
        } catch (Exception e) {
            log.warn("初始化全局表单数据失败: workflowCode=" + workflowCode, e);
        }
    }
}
