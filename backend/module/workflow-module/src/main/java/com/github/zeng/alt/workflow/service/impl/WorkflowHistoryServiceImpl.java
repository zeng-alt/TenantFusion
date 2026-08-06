package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.HistoricActivityVO;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceVO;
import com.github.zeng.alt.workflow.model.HistoricVariableVO;
import com.github.zeng.alt.workflow.model.TaskVO;
import com.github.zeng.alt.workflow.service.WorkflowHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 工作流历史服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class WorkflowHistoryServiceImpl implements WorkflowHistoryService {

    private final HistoryService historyService;

    @Override
    public PageRestResponse<HistoricProcessInstanceVO> queryHistoricInstances(
            String processDefinitionKey, String businessKey, Boolean finished,
            String startUserId, int pageNum, int pageSize) {

        HistoricProcessInstanceQuery camundaQuery = historyService.createHistoricProcessInstanceQuery();

        if (processDefinitionKey != null && !processDefinitionKey.isBlank()) {
            camundaQuery.processDefinitionKey(processDefinitionKey);
        }
        if (businessKey != null && !businessKey.isBlank()) {
            camundaQuery.processInstanceBusinessKey(businessKey);
        }
        if (finished != null && finished) {
            camundaQuery.finished();
        } else if (finished != null && !finished) {
            camundaQuery.unfinished();
        }
        if (startUserId != null && !startUserId.isBlank()) {
            camundaQuery.startedBy(startUserId);
        }

        camundaQuery.orderByProcessInstanceStartTime().desc();

        long total = camundaQuery.count();
        int firstResult = (pageNum - 1) * pageSize;
        List<HistoricProcessInstance> list = camundaQuery.listPage(firstResult, pageSize);
        List<HistoricProcessInstanceVO> vos = list.stream().map(this::toVO).toList();

        return PageRestResponse.of(vos, total, pageSize, pageNum);
    }

    @Override
    public HistoricProcessInstanceVO getHistoricInstance(String id) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id)
                .singleResult();
        if (hpi == null) {
            throw new RuntimeException("历史流程实例不存在: " + id);
        }
        return toVO(hpi);
    }

    @Override
    public PageRestResponse<TaskVO> queryHistoricTasks(
            String assignee, String processInstanceId, Boolean finished,
            int pageNum, int pageSize) {

        HistoricTaskInstanceQuery camundaQuery = historyService.createHistoricTaskInstanceQuery();

        if (assignee != null && !assignee.isBlank()) {
            camundaQuery.taskAssignee(assignee);
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            camundaQuery.processInstanceId(processInstanceId);
        }
        if (finished != null && finished) {
            camundaQuery.finished();
        } else if (finished != null && !finished) {
            camundaQuery.unfinished();
        }

        camundaQuery.orderByHistoricTaskInstanceEndTime().desc();

        long total = camundaQuery.count();
        int firstResult = (pageNum - 1) * pageSize;
        List<HistoricTaskInstance> list = camundaQuery.listPage(firstResult, pageSize);
        List<TaskVO> vos = list.stream().map(this::toTaskVO).toList();

        return PageRestResponse.of(vos, total, pageSize, pageNum);
    }

    @Override
    public List<HistoricActivityVO> queryHistoricActivities(String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        return activities.stream().map(act -> HistoricActivityVO.builder()
                .id(act.getId())
                .activityName(act.getActivityName())
                .activityType(act.getActivityType())
                .startTime(toLocalDateTime(act.getStartTime()))
                .endTime(toLocalDateTime(act.getEndTime()))
                .durationInMillis(act.getDurationInMillis())
                .assignee(act.getAssignee())
                .taskId(act.getTaskId())
                .processInstanceId(act.getProcessInstanceId())
                .processDefinitionId(act.getProcessDefinitionId())
                .build()).toList();
    }

    @Override
    public PageRestResponse<HistoricVariableVO> queryHistoricVariables(
            String processInstanceId, String variableName, int pageNum, int pageSize) {

        HistoricVariableInstanceQuery camundaQuery = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId);

        if (variableName != null && !variableName.isBlank()) {
            camundaQuery.variableName(variableName);
        }

        long total = camundaQuery.count();
        int firstResult = (pageNum - 1) * pageSize;
        List<HistoricVariableInstance> list = camundaQuery.listPage(firstResult, pageSize);
        List<HistoricVariableVO> vos = list.stream().map(v -> HistoricVariableVO.builder()
                .name(v.getName())
                .value(v.getValue())
                .typeName(v.getTypeName())
                .processInstanceId(v.getProcessInstanceId())
                .taskId(v.getTaskId())
                .activityInstanceId(v.getActivityInstanceId())
                .createTime(toLocalDateTime(v.getCreateTime()))
//                .lastUpdatedTime(toLocalDateTime(v.getLastUpdatedTime()))
                .build()).toList();

        return PageRestResponse.of(vos, total, pageSize, pageNum);
    }

    private HistoricProcessInstanceVO toVO(HistoricProcessInstance hpi) {
        HistoricProcessInstanceVO.HistoricProcessInstanceVOBuilder builder = HistoricProcessInstanceVO.builder()
                .id(hpi.getId())
                .businessKey(hpi.getBusinessKey())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(hpi.getProcessDefinitionName())
                .startTime(toLocalDateTime(hpi.getStartTime()))
                .endTime(toLocalDateTime(hpi.getEndTime()))
                .durationInMillis(hpi.getDurationInMillis())
                .startUserId(hpi.getStartUserId())
                .deleteReason(hpi.getDeleteReason())
                .tenantId(hpi.getTenantId());

        if (hpi.getEndTime() != null) {
            if (hpi.getDeleteReason() != null) {
                builder.state("deleted");
            } else {
                builder.state("completed");
            }
        } else {
            builder.state("active");
        }

        return builder.build();
    }

    private TaskVO toTaskVO(HistoricTaskInstance task) {
        TaskVO.TaskVOBuilder builder = TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .executionId(task.getExecutionId())
                .processDefinitionId(task.getProcessDefinitionId())
                .tenantId(task.getTenantId())
                .createTime(toLocalDateTime(task.getStartTime()))
                .dueDate(toLocalDateTime(task.getDueDate()));

        if (task.getPriority() > 0) {
            builder.priority(task.getPriority());
        }

        return builder.build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
