package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceApi;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInstanceQuery;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.api.process.StartProcessApi;
import com.github.zeng.alt.workflow.model.ProcessInstanceVO;
import com.github.zeng.alt.workflow.model.StartProcessCmd;
import com.github.zeng.alt.workflow.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 流程实例服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private final ProcessInstanceApi processInstanceApi;
    private final StartProcessApi startProcessApi;

    @Override
    public PageRestResponse<ProcessInstanceVO> queryInstances(
            com.github.zeng.alt.workflow.model.ProcessInstanceQuery q) {
        PageRestResponse<ProcessInstanceInfo> page = processInstanceApi.query(ProcessInstanceQuery.builder()
                .processDefinitionKey(q.getProcessDefinitionKey())
                .businessKey(q.getBusinessKey())
                .suspended(q.getSuspended())
                .tenantId(q.getTenantId())
                .pageNo(q.getPageNo())
                .pageSize(q.getPageSize())
                .build());
        List<ProcessInstanceVO> vos = page.getData().getPageData().stream().map(this::toVO).toList();
        return PageRestResponse.of(vos, page.getData().getTotal(), q.getPageSize(), q.getPageNo());
    }

    @Override
    public ProcessInstanceVO getInstance(String id) {
        return toVO(processInstanceApi.get(id));
    }

    @Override
    public ProcessInstanceVO startProcess(StartProcessCmd cmd) {
        ProcessInformation info = startProcessApi.startByDefinition(StartByDefinitionCmd.builder()
                .processDefinitionKey(cmd.getProcessDefinitionKey())
                .businessKey(cmd.getBusinessKey())
                .variables(cmd.getVariables())
                .initiator(cmd.getStartUserId())
                .build());
        log.info("启动流程实例: " + info.getInstanceId() + ", 定义: " + cmd.getProcessDefinitionKey()
                + ", 发起人: " + cmd.getStartUserId());
        return toVO(processInstanceApi.get(info.getInstanceId()));
    }

    @Override
    public void suspendInstance(String id) {
        processInstanceApi.suspend(id);
        log.info("挂起流程实例: " + id);
    }

    @Override
    public void activateInstance(String id) {
        processInstanceApi.activate(id);
        log.info("激活流程实例: " + id);
    }

    @Override
    public void deleteInstance(String id, String reason) {
        processInstanceApi.delete(id, reason);
        log.info("删除流程实例: " + id + ", 原因: " + reason);
    }

    @Override
    public Map<String, Object> getVariables(String id) {
        return processInstanceApi.getVariables(id);
    }

    @Override
    public void setVariables(String id, Map<String, Object> variables) {
        processInstanceApi.setVariables(id, variables);
        log.info("设置流程变量: " + id);
    }

    private ProcessInstanceVO toVO(ProcessInstanceInfo info) {
        if (info == null) {
            return null;
        }
        return ProcessInstanceVO.builder()
                .id(info.getId())
                .businessKey(info.getBusinessKey())
                .processDefinitionId(info.getProcessDefinitionId())
                .processDefinitionKey(info.getProcessDefinitionKey())
                .processDefinitionName(info.getProcessDefinitionName())
                .processDefinitionVersion(info.getProcessDefinitionVersion())
                .suspended(info.getSuspended())
                .ended(info.getEnded())
                .tenantId(info.getTenantId())
                .build();
    }
}
