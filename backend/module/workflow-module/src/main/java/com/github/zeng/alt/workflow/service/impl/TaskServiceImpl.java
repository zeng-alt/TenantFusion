package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.CompleteTaskCmd;
import com.github.zeng.alt.workflow.model.TaskCommentVO;
import com.github.zeng.alt.workflow.model.TaskQuery;
import com.github.zeng.alt.workflow.model.TaskVO;
import com.github.zeng.alt.workflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户任务服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final org.camunda.bpm.engine.TaskService camundaTaskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    @Override
    public PageRestResponse<TaskVO> queryTasks(TaskQuery query) {
        org.camunda.bpm.engine.task.TaskQuery camundaQuery = camundaTaskService.createTaskQuery();

        if (query.getName() != null && !query.getName().isBlank()) {
            camundaQuery.taskNameLike("%" + query.getName() + "%");
        }
        if (query.getProcessDefinitionName() != null && !query.getProcessDefinitionName().isBlank()) {
            camundaQuery.processDefinitionNameLike("%" + query.getProcessDefinitionName() + "%");
        }
        if (query.getUserId() != null && !query.getUserId().isBlank()) {
            camundaQuery.or()
                    .taskAssignee(query.getUserId())
                    .taskCandidateUser(query.getUserId())
                    .endOr();
        }
        if (query.getInitiator() != null && !query.getInitiator().isBlank()) {
            List<String> instanceIds = historyService.createHistoricProcessInstanceQuery()
                    .startedBy(query.getInitiator()).list()
                    .stream().map(HistoricProcessInstance::getId).toList();
            if (instanceIds.isEmpty()) {
                return PageRestResponse.of(List.<TaskVO>of(), 0L, query.getPageSize(), query.getPage());
            }
            camundaQuery.processInstanceIdIn(instanceIds.toArray(new String[0]));
        }
        if (query.getTaskDefinitionKey() != null && !query.getTaskDefinitionKey().isBlank()) {
            camundaQuery.taskDefinitionKey(query.getTaskDefinitionKey());
        }
        if (query.getAssignee() != null && !query.getAssignee().isBlank()) {
            camundaQuery.taskAssignee(query.getAssignee());
        }
        if (query.getCandidateUser() != null && !query.getCandidateUser().isBlank()) {
            camundaQuery.taskCandidateUser(query.getCandidateUser());
        }
        if (query.getCandidateGroup() != null && !query.getCandidateGroup().isBlank()) {
            camundaQuery.taskCandidateGroup(query.getCandidateGroup());
        }
        if (query.getProcessInstanceId() != null && !query.getProcessInstanceId().isBlank()) {
            camundaQuery.processInstanceId(query.getProcessInstanceId());
        }
        if (query.getProcessDefinitionKey() != null && !query.getProcessDefinitionKey().isBlank()) {
            camundaQuery.processDefinitionKey(query.getProcessDefinitionKey());
        }
        if (query.getBusinessKey() != null && !query.getBusinessKey().isBlank()) {
            camundaQuery.processInstanceBusinessKey(query.getBusinessKey());
        }
        if (query.getSuspended() != null && query.getSuspended()) {
            camundaQuery.suspended();
        } else if (query.getSuspended() != null && !query.getSuspended()) {
            camundaQuery.active();
        }
        if (query.getUnassigned() != null && query.getUnassigned()) {
            camundaQuery.taskUnassigned();
        }
//        if (query.getTenantId() != null && !query.getTenantId().isBlank()) {
//            camundaQuery.taskTenantId(query.getTenantId());
//        }

        camundaQuery.orderByTaskCreateTime().desc();

        long total = camundaQuery.count();
        int firstResult = (query.getPage() - 1) * query.getPageSize();
        List<Task> list = camundaQuery.listPage(firstResult, query.getPageSize());
        List<TaskVO> vos = list.stream().map(this::toVO).toList();

        Map<String, String> initiators = loadInitiatorMap(
                list.stream().map(Task::getProcessInstanceId).filter(Objects::nonNull).distinct().toList());
        vos.forEach(vo -> vo.setInitiator(initiators.get(vo.getProcessInstanceId())));

        return PageRestResponse.of(vos, total, query.getPageSize(), query.getPage());
    }

    @Override
    public TaskVO getTask(String id) {
        Task task = camundaTaskService.createTaskQuery().taskId(id).singleResult();
        TaskVO vo;
        if (task == null) {
            // 尝试从历史中查询
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(id).singleResult();
            if (historicTask != null) {
                vo = toHistoricVO(historicTask);
            } else {
                throw new RuntimeException("任务不存在: " + id);
            }
        } else {
            vo = toVO(task);
        }
        if (vo.getProcessInstanceId() != null) {
            vo.setInitiator(loadInitiatorMap(List.of(vo.getProcessInstanceId())).get(vo.getProcessInstanceId()));
        }
        return vo;
    }

    @Override
    public void claimTask(String taskId, String userId) {
        camundaTaskService.claim(taskId, userId);
        log.info("签收任务: " + taskId + ", 用户: " + userId);
    }

    @Override
    public void unclaimTask(String taskId) {
        Task task = camundaTaskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            camundaTaskService.setAssignee(taskId, null);
            log.info("取消签收任务: " + taskId);
        }
    }

    @Override
    public void completeTask(CompleteTaskCmd cmd) {
        if (cmd.getComment() != null && !cmd.getComment().isBlank()) {
            Task task = camundaTaskService.createTaskQuery().taskId(cmd.getTaskId()).singleResult();
            if (task != null) {
                camundaTaskService.createComment(cmd.getTaskId(), task.getProcessInstanceId(), cmd.getComment());
            }
        }
        if (cmd.getVariables() != null && !cmd.getVariables().isEmpty()) {
            camundaTaskService.complete(cmd.getTaskId(), cmd.getVariables());
        } else {
            camundaTaskService.complete(cmd.getTaskId());
        }
        log.info("完成任务: " + cmd.getTaskId());
    }

    @Override
    public void delegateTask(String taskId, String userId) {
        camundaTaskService.delegateTask(taskId, userId);
        log.info("委派任务: " + taskId + " -> " + userId);
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables) {
        if (variables != null && !variables.isEmpty()) {
            camundaTaskService.resolveTask(taskId, variables);
        } else {
            camundaTaskService.resolveTask(taskId);
        }
        log.info("解决委派任务: " + taskId);
    }

    @Override
    public void assignTask(String taskId, String userId) {
        camundaTaskService.setAssignee(taskId, userId);
        log.info("分配任务: " + taskId + " -> " + userId);
    }

    @Override
    public PageRestResponse<TaskCommentVO> getComments(String taskId, int pageNum, int pageSize) {
        List<Comment> taskComments = camundaTaskService.getTaskComments(taskId);
        int fromIndex = Math.min((pageNum - 1) * pageSize, taskComments.size());
        int toIndex = Math.min(fromIndex + pageSize, taskComments.size());
        List<TaskCommentVO> vos = taskComments.subList(fromIndex, toIndex)
                .stream().map(this::toCommentVO).toList();

        return PageRestResponse.of(vos, (long) taskComments.size(), pageSize, pageNum);
    }

    @Override
    public void addComment(String taskId, String processInstanceId, String message) {
        camundaTaskService.createComment(taskId, processInstanceId, message);
        log.info("添加批注: task=" + taskId + ", msg=" + message);
    }

    private TaskVO toVO(Task task) {
        ProcessInstance pi = null;
        ProcessDefinition pd = null;
        if (task.getProcessInstanceId() != null) {
            pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (task.getProcessDefinitionId() != null) {
                pd = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(task.getProcessDefinitionId())
                        .singleResult();
            }
        }

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
                .suspended(task.isSuspended());

        if (task.getCreateTime() != null) {
            builder.createTime(LocalDateTime.ofInstant(
                    task.getCreateTime().toInstant(), ZoneId.systemDefault()));
        }
        if (task.getDueDate() != null) {
            builder.dueDate(LocalDateTime.ofInstant(
                    task.getDueDate().toInstant(), ZoneId.systemDefault()));
        }
        if (task.getFollowUpDate() != null) {
            builder.followUpDate(LocalDateTime.ofInstant(
                    task.getFollowUpDate().toInstant(), ZoneId.systemDefault()));
        }
        if (pi != null) {
            builder.businessKey(pi.getBusinessKey());
        }
        if (pd != null) {
            builder.processDefinitionKey(pd.getKey())
                    .processDefinitionName(pd.getName());
        }
        if (task.getPriority() > 0) {
            builder.priority(task.getPriority());
        }

        return builder.build();
    }

    private TaskVO toHistoricVO(HistoricTaskInstance task) {
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
                .tenantId(task.getTenantId());

        if (task.getStartTime() != null) {
            builder.createTime(LocalDateTime.ofInstant(
                    task.getStartTime().toInstant(), ZoneId.systemDefault()));
        }
        if (task.getDueDate() != null) {
            builder.dueDate(LocalDateTime.ofInstant(
                    task.getDueDate().toInstant(), ZoneId.systemDefault()));
        }
        if (task.getPriority() > 0) {
            builder.priority(task.getPriority());
        }

        return builder.build();
    }

    private TaskCommentVO toCommentVO(Comment comment) {
        return TaskCommentVO.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .processInstanceId(comment.getProcessInstanceId())
                .userId(comment.getUserId())
                .message(comment.getFullMessage())
                .time(comment.getTime() != null
                        ? LocalDateTime.ofInstant(comment.getTime().toInstant(), ZoneId.systemDefault())
                        : null)
                .build();
    }

    private Map<String, String> loadInitiatorMap(List<String> processInstanceIds) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (String id : processInstanceIds) {
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(id).singleResult();
            if (hpi != null) {
                map.put(id, hpi.getStartUserId());
            }
        }
        return map;
    }
}
