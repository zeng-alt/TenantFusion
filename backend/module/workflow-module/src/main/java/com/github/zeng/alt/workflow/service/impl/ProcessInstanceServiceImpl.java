package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.ProcessInstanceVO;
import com.github.zeng.alt.workflow.model.StartProcessCmd;
import com.github.zeng.alt.workflow.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final IdentityService identityService;

    @Override
    public PageRestResponse<ProcessInstanceVO> queryInstances(
            com.github.zeng.alt.workflow.model.ProcessInstanceQuery q) {

        ProcessInstanceQuery camundaQuery = runtimeService.createProcessInstanceQuery();

        if (q.getProcessDefinitionKey() != null && !q.getProcessDefinitionKey().isBlank()) {
            camundaQuery.processDefinitionKey(q.getProcessDefinitionKey());
        }
        if (q.getBusinessKey() != null && !q.getBusinessKey().isBlank()) {
            camundaQuery.processInstanceBusinessKey(q.getBusinessKey());
        }
        if (q.getSuspended() != null && q.getSuspended()) {
            camundaQuery.suspended();
        } else if (q.getSuspended() != null && !q.getSuspended()) {
            camundaQuery.active();
        }
        if (q.getTenantId() != null && !q.getTenantId().isBlank()) {
            camundaQuery.tenantIdIn(q.getTenantId());
        }

        camundaQuery.orderByProcessInstanceId().desc();

        long total = camundaQuery.count();
        int firstResult = (q.getPage() - 1) * q.getPageSize();
        List<ProcessInstance> list = camundaQuery.listPage(firstResult, q.getPageSize());
        List<ProcessInstanceVO> vos = list.stream().map(this::toVO).toList();

        return PageRestResponse.of(vos, total, q.getPageSize(), q.getPage());
    }

    @Override
    public ProcessInstanceVO getInstance(String id) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(id)
                .singleResult();
        if (pi == null) {
            throw new RuntimeException("流程实例不存在: " + id);
        }
        return toVO(pi);
    }

    @Override
    public ProcessInstanceVO startProcess(StartProcessCmd cmd) {
        ProcessInstance pi;
        boolean authenticated = cmd.getStartUserId() != null && !cmd.getStartUserId().isBlank();
        if (authenticated) {
            identityService.setAuthenticatedUserId(cmd.getStartUserId());
        }
        try {
            if (cmd.getBusinessKey() != null && !cmd.getBusinessKey().isBlank()) {
                pi = runtimeService.startProcessInstanceByKey(
                        cmd.getProcessDefinitionKey(),
                        cmd.getBusinessKey(),
                        cmd.getVariables());
            } else {
                pi = runtimeService.startProcessInstanceByKey(
                        cmd.getProcessDefinitionKey(),
                        cmd.getVariables());
            }
        } finally {
            if (authenticated) {
                identityService.clearAuthentication();
            }
        }
        log.info("启动流程实例: " + pi.getId() + ", 定义: " + cmd.getProcessDefinitionKey()
                + ", 发起人: " + cmd.getStartUserId());
        return toVO(pi);
    }

    @Override
    public void suspendInstance(String id) {
        runtimeService.suspendProcessInstanceById(id);
        log.info("挂起流程实例: " + id);
    }

    @Override
    public void activateInstance(String id) {
        runtimeService.activateProcessInstanceById(id);
        log.info("激活流程实例: " + id);
    }

    @Override
    public void deleteInstance(String id, String reason) {
        runtimeService.deleteProcessInstance(id, reason);
        log.info("删除流程实例: " + id + ", 原因: " + reason);
    }

    @Override
    public Map<String, Object> getVariables(String id) {
        return runtimeService.getVariables(id);
    }

    @Override
    public void setVariables(String id, Map<String, Object> variables) {
        runtimeService.setVariables(id, variables);
        log.info("设置流程变量: " + id);
    }

    private ProcessInstanceVO toVO(ProcessInstance pi) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(pi.getProcessDefinitionId())
                .singleResult();

        ProcessInstanceVO.ProcessInstanceVOBuilder builder = ProcessInstanceVO.builder()
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
//        if (pi.getStartTime() != null) {
//            builder.startTime(LocalDateTime.ofInstant(pi.getStartTime().toInstant(), ZoneId.systemDefault()));
//        }

        return builder.build();
    }
}
