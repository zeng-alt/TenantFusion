package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.history.HistoricActivityInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceQuery;
import com.github.zeng.alt.camunda.engine.api.history.HistoricTaskInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricVariableInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoryApi;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.workflow.mapper.WorkflowHistoryMapper;
import com.github.zeng.alt.workflow.model.ExecutionStatus;
import com.github.zeng.alt.workflow.model.FlowExecutionState;
import com.github.zeng.alt.workflow.model.HistoricActivityVO;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceVO;
import com.github.zeng.alt.workflow.model.HistoricVariableVO;
import com.github.zeng.alt.workflow.model.NodeExecutionState;
import com.github.zeng.alt.workflow.model.ProcessExecutionState;
import com.github.zeng.alt.workflow.model.TaskVO;
import com.github.zeng.alt.workflow.service.TaskFormResolver;
import com.github.zeng.alt.workflow.service.WorkflowHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工作流历史服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class WorkflowHistoryServiceImpl implements WorkflowHistoryService {

    private final HistoryApi historyApi;
    private final TaskApi taskApi;
    private final ProcessDefinitionApi processDefinitionApi;
    private final WorkflowHistoryMapper workflowHistoryMapper;
    private final TaskFormResolver taskFormResolver;

    @Override
    public PageRestResponse<HistoricProcessInstanceVO> queryHistoricInstances(
            com.github.zeng.alt.workflow.model.HistoricProcessInstanceQuery query) {

        PageRestResponse<HistoricProcessInstanceInfo> page = historyApi.queryProcessInstances(
                HistoricProcessInstanceQuery.builder()
                        .processDefinitionKey(query.getProcessDefinitionKey())
                        .processDefinitionName(query.getProcessDefinitionName())
                        .businessKey(query.getBusinessKey())
                        .state(query.getState())
                        .startUserId(query.getStartUserId())
                        .pageNo(query.getPageNo())
                        .pageSize(query.getPageSize())
                        .build());

        List<HistoricProcessInstanceVO> vos = page.getData().getPageData().stream()
                .map(workflowHistoryMapper::toProcessInstanceVO).toList();

        Map<String, String[]> currentTasks = loadCurrentTasks(vos);
        vos.forEach(vo -> {
            String[] current = currentTasks.get(vo.getId());
            if (current != null) {
                vo.setCurrentTaskName(current[0]);
                vo.setCurrentAssignee(current[1]);
            }
        });

        return PageRestResponse.of(vos, page.getData().getTotal(), query.getPageSize(), query.getPageNo());
    }

    /**
     * 批量加载进行中流程实例的当前节点与处理人
     */
    private Map<String, String[]> loadCurrentTasks(List<HistoricProcessInstanceVO> vos) {
        List<String> runningIds = vos.stream()
                .filter(vo -> vo.getEndTime() == null)
                .map(HistoricProcessInstanceVO::getId)
                .toList();
        if (runningIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<TaskInfo>> tasksByPi = runningIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> taskApi.getActiveTasks(id),
                (a, b) -> a,
                LinkedHashMap::new));

        return tasksByPi.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    String names = entry.getValue().stream()
                            .map(TaskInfo::getName).filter(Objects::nonNull).distinct()
                            .collect(Collectors.joining("、"));
                    String assignees = entry.getValue().stream()
                            .map(TaskInfo::getAssignee).filter(Objects::nonNull).distinct()
                            .collect(Collectors.joining("、"));
                    return new String[]{names, assignees};
                }));
    }

    @Override
    public HistoricProcessInstanceVO getHistoricInstance(String id) {
        HistoricProcessInstanceInfo info = historyApi.getProcessInstance(id);
        HistoricProcessInstanceVO vo = workflowHistoryMapper.toProcessInstanceVO(info);
        vo.setBpmnXml(loadBpmnXml(info.getProcessDefinitionKey(), info.getProcessDefinitionId()));
        vo.setProcessForm(loadProcessForm(id));
        vo.setCurrentTaskForms(taskFormResolver.resolveByProcessInstanceId(id));
        vo.setConfigForm(taskFormResolver.resolveConfigForm(id));
        vo.setExecutionState(buildExecutionState(id));
        String[] current = loadCurrentTask(id);
        if (current != null) {
            vo.setCurrentTaskName(current[0]);
            vo.setCurrentAssignee(current[1]);
        }
        return vo;
    }

    /**
     * 加载单个流程实例当前活动节点与处理人
     */
    private String[] loadCurrentTask(String processInstanceId) {
        List<TaskInfo> activeTasks = taskApi.getActiveTasks(processInstanceId);
        if (activeTasks.isEmpty()) {
            return null;
        }
        String names = activeTasks.stream()
                .map(TaskInfo::getName).filter(Objects::nonNull).distinct()
                .collect(Collectors.joining("、"));
        String assignees = activeTasks.stream()
                .map(TaskInfo::getAssignee).filter(Objects::nonNull).distinct()
                .collect(Collectors.joining("、"));
        return new String[]{names, assignees};
    }

    /**
     * 由历史活动构建 BPMN 执行状态（供 BpmnProcessViewer 高亮/时间线使用）
     */
    private ProcessExecutionState buildExecutionState(String processInstanceId) {
        List<HistoricActivityInfo> activities = historyApi.activities(processInstanceId);
        Map<String, NodeExecutionState> elements = new LinkedHashMap<>();
        List<String> executionOrder = new ArrayList<>();
        List<String> timestamps = new ArrayList<>();
        for (HistoricActivityInfo act : activities) {
            String id = act.getActivityId();
            if (id == null || id.isBlank()) {
                continue;
            }
            boolean done = act.getEndTime() != null;
            NodeExecutionState prev = elements.get(id);
            elements.put(id, NodeExecutionState.builder()
                    .status(done ? ExecutionStatus.completed : ExecutionStatus.active)
                    .visitCount((prev == null ? 0 : prev.getVisitCount()) + 1)
                    .rejectCount(prev == null ? 0 : prev.getRejectCount())
                    .assignee(act.getAssignee() != null ? act.getAssignee() : (prev == null ? null : prev.getAssignee()))
                    .build());
            executionOrder.add(id);
            timestamps.add(formatDateTime(done ? act.getEndTime() : act.getStartTime()));
        }
        return ProcessExecutionState.builder()
                .processInstanceId(processInstanceId)
                .elements(elements)
                .executionOrder(executionOrder)
                .timestamps(timestamps)
                .build();
    }

    private static final DateTimeFormatter EXECUTION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : EXECUTION_TIME_FORMATTER.format(time);
    }

    /**
     * 加载流程定义部署的原始 BPMN XML
     */
    private String loadBpmnXml(String processDefinitionKey, String processDefinitionId) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        try {
            return new String(processDefinitionApi.getBpmnXml(processDefinitionId), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("加载BPMN XML失败: " + processDefinitionId + "(" + processDefinitionKey + ")", e);
            return null;
        }
    }

    /**
     * 加载流程启动时注入的表单数据（processForm 流程变量）
     */
    private Object loadProcessForm(String processInstanceId) {
        return historyApi.variables(processInstanceId).stream()
                .filter(v -> "processForm".equals(v.getName()))
                .findFirst()
                .map(HistoricVariableInfo::getValue)
                .orElse(null);
    }

    @Override
    public PageRestResponse<TaskVO> queryHistoricTasks(
            String assignee, String processInstanceId, Boolean finished,
            int pageNum, int pageSize) {

        PageRestResponse<HistoricTaskInfo> page = historyApi.queryTasks(
                assignee, processInstanceId, finished, pageNum, pageSize);
        List<TaskVO> vos = page.getData().getPageData().stream()
                .map(workflowHistoryMapper::toTaskVO).toList();

        return PageRestResponse.of(vos, page.getData().getTotal(), pageSize, pageNum);
    }

    @Override
    public List<HistoricActivityVO> queryHistoricActivities(String processInstanceId) {
        return historyApi.activities(processInstanceId).stream()
                .map(workflowHistoryMapper::toActivityVO).toList();
    }

    @Override
    public PageRestResponse<HistoricVariableVO> queryHistoricVariables(
            String processInstanceId, String variableName, int pageNum, int pageSize) {

        PageRestResponse<HistoricVariableInfo> page = historyApi.queryVariables(
                processInstanceId, variableName, pageNum, pageSize);
        List<HistoricVariableVO> vos = page.getData().getPageData().stream()
                .map(workflowHistoryMapper::toVariableVO).toList();

        return PageRestResponse.of(vos, page.getData().getTotal(), pageSize, pageNum);
    }
}
