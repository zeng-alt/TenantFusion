package com.github.zeng.alt.camunda.engine.embedded.history;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.history.HistoricActivityInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceQuery;
import com.github.zeng.alt.camunda.engine.api.history.HistoricTaskInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricVariableInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoryApi;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 嵌入式历史数据实现
 * <p>
 * 发起人取 START_USER_ID_（嵌入式下与 initiator 变量一致）。
 *
 * @author zengAlt
 */
@Service
public class EmbeddedHistoryApi implements HistoryApi {

    private final HistoryService historyService;

    public EmbeddedHistoryApi(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public PageRestResponse<HistoricProcessInstanceInfo> queryProcessInstances(HistoricProcessInstanceQuery query) {
        org.camunda.bpm.engine.history.HistoricProcessInstanceQuery camundaQuery =
                historyService.createHistoricProcessInstanceQuery();

        if (StringUtils.hasText(query.getProcessDefinitionKey())) {
            camundaQuery.processDefinitionKey(query.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(query.getProcessDefinitionName())) {
            camundaQuery.processDefinitionNameLike("%" + query.getProcessDefinitionName() + "%");
        }
        if (StringUtils.hasText(query.getBusinessKey())) {
            camundaQuery.processInstanceBusinessKey(query.getBusinessKey());
        }
        if (StringUtils.hasText(query.getState())) {
            switch (query.getState()) {
                case "running" -> camundaQuery.unfinished();
                case "completed", "terminated" -> camundaQuery.finished();
                case "suspended" -> camundaQuery.suspended();
                default -> {
                }
            }
        }
        if (StringUtils.hasText(query.getStartUserId())) {
            camundaQuery.startedBy(query.getStartUserId());
        }
        if (StringUtils.hasText(query.getInitiator())) {
            camundaQuery.startedBy(query.getInitiator());
        }

        camundaQuery.orderByProcessInstanceStartTime().desc();

        long total = camundaQuery.count();
        int firstResult = (query.getPageNo() - 1) * query.getPageSize();
        List<HistoricProcessInstance> list = camundaQuery.listPage(firstResult, query.getPageSize());
        List<HistoricProcessInstanceInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, query.getPageSize(), query.getPageNo());
    }

    @Override
    public HistoricProcessInstanceInfo getProcessInstance(String processInstanceId) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (hpi == null) {
            throw new IllegalStateException("历史流程实例不存在: " + processInstanceId);
        }
        return toInfo(hpi);
    }

    @Override
    public PageRestResponse<HistoricTaskInfo> queryTasks(String assignee, String processInstanceId, Boolean finished,
                                                         int pageNo, int pageSize) {
        org.camunda.bpm.engine.history.HistoricTaskInstanceQuery camundaQuery =
                historyService.createHistoricTaskInstanceQuery();
        if (StringUtils.hasText(assignee)) {
            camundaQuery.taskAssignee(assignee);
        }
        if (StringUtils.hasText(processInstanceId)) {
            camundaQuery.processInstanceId(processInstanceId);
        }
        if (finished != null && finished) {
            camundaQuery.finished();
        } else if (finished != null && !finished) {
            camundaQuery.unfinished();
        }
        camundaQuery.orderByHistoricTaskInstanceEndTime().desc();

        long total = camundaQuery.count();
        int firstResult = (pageNo - 1) * pageSize;
        List<HistoricTaskInstance> list = camundaQuery.listPage(firstResult, pageSize);
        List<HistoricTaskInfo> vos = list.stream().map(this::toTaskInfo).toList();
        return PageRestResponse.of(vos, total, pageSize, pageNo);
    }

    @Override
    public List<HistoricActivityInfo> activities(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list().stream().map(this::toActivityInfo).toList();
    }

    @Override
    public PageRestResponse<HistoricVariableInfo> queryVariables(String processInstanceId, String variableName,
                                                                 int pageNo, int pageSize) {
        org.camunda.bpm.engine.history.HistoricVariableInstanceQuery camundaQuery =
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId);
        if (StringUtils.hasText(variableName)) {
            camundaQuery.variableName(variableName);
        }
        long total = camundaQuery.count();
        int firstResult = (pageNo - 1) * pageSize;
        List<HistoricVariableInstance> list = camundaQuery.listPage(firstResult, pageSize);
        List<HistoricVariableInfo> vos = list.stream().map(this::toVariableInfo).toList();
        return PageRestResponse.of(vos, total, pageSize, pageNo);
    }

    @Override
    public List<HistoricVariableInfo> variables(String processInstanceId) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().stream().map(this::toVariableInfo).toList();
    }

    private HistoricProcessInstanceInfo toInfo(HistoricProcessInstance hpi) {
        return HistoricProcessInstanceInfo.builder()
                .id(hpi.getId())
                .businessKey(hpi.getBusinessKey())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(hpi.getProcessDefinitionName())
                .processDefinitionVersion(hpi.getProcessDefinitionVersion())
                .startTime(hpi.getStartTime() != null ? toLocalDateTime(hpi.getStartTime()) : null)
                .endTime(hpi.getEndTime() != null ? toLocalDateTime(hpi.getEndTime()) : null)
                .durationInMillis(hpi.getDurationInMillis())
                .startUserId(hpi.getStartUserId())
                .initiator(hpi.getStartUserId())
                .deleteReason(hpi.getDeleteReason())
                .tenantId(hpi.getTenantId())
                .build();
    }

    private HistoricTaskInfo toTaskInfo(HistoricTaskInstance task) {
        return HistoricTaskInfo.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .startTime(task.getStartTime() != null ? toLocalDateTime(task.getStartTime()) : null)
                .endTime(task.getEndTime() != null ? toLocalDateTime(task.getEndTime()) : null)
                .dueDate(task.getDueDate() != null ? toLocalDateTime(task.getDueDate()) : null)
                .priority(task.getPriority())
                .build();
    }

    private HistoricActivityInfo toActivityInfo(HistoricActivityInstance act) {
        return HistoricActivityInfo.builder()
                .id(act.getId())
                .activityId(act.getActivityId())
                .activityName(act.getActivityName())
                .activityType(act.getActivityType())
                .assignee(act.getAssignee())
                .processInstanceId(act.getProcessInstanceId())
                .executionId(act.getExecutionId())
                .taskId(act.getTaskId())
                .startTime(act.getStartTime() != null ? toLocalDateTime(act.getStartTime()) : null)
                .endTime(act.getEndTime() != null ? toLocalDateTime(act.getEndTime()) : null)
                .durationInMillis(act.getDurationInMillis())
                .build();
    }

    private HistoricVariableInfo toVariableInfo(HistoricVariableInstance variable) {
        return HistoricVariableInfo.builder()
                .id(variable.getId())
                .name(variable.getName())
                .value(variable.getValue())
                .type(variable.getTypeName())
                .processInstanceId(variable.getProcessInstanceId())
                .executionId(variable.getExecutionId())
                .activityInstanceId(variable.getActivityInstanceId())
                .build();
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
