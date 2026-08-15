package com.github.zeng.alt.camunda.engine.embedded.task;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.task.CommentInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskQuery;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 嵌入式用户任务实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedTaskApi implements TaskApi {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    public EmbeddedTaskApi(TaskService taskService, RuntimeService runtimeService,
                           RepositoryService repositoryService, HistoryService historyService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
    }

    @Override
    public PageRestResponse<TaskInfo> query(TaskQuery query) {
        org.camunda.bpm.engine.task.TaskQuery camundaQuery = taskService.createTaskQuery();

        if (StringUtils.hasText(query.getName())) {
            camundaQuery.taskNameLike("%" + query.getName() + "%");
        }
        if (StringUtils.hasText(query.getProcessDefinitionName())) {
            camundaQuery.processDefinitionNameLike("%" + query.getProcessDefinitionName() + "%");
        }
        if (StringUtils.hasText(query.getUserId())) {
            camundaQuery.or()
                    .taskAssignee(query.getUserId())
                    .taskCandidateUser(query.getUserId())
                    .endOr();
        }
        if (StringUtils.hasText(query.getInitiator())) {
            List<String> instanceIds = historyService.createHistoricProcessInstanceQuery()
                    .startedBy(query.getInitiator()).list()
                    .stream().map(HistoricProcessInstance::getId).toList();
            if (instanceIds.isEmpty()) {
                return PageRestResponse.of(List.<TaskInfo>of(), 0L, query.getPageSize(), query.getPageNo());
            }
            camundaQuery.processInstanceIdIn(instanceIds.toArray(new String[0]));
        }
        if (StringUtils.hasText(query.getTaskDefinitionKey())) {
            camundaQuery.taskDefinitionKey(query.getTaskDefinitionKey());
        }
        if (StringUtils.hasText(query.getAssignee())) {
            camundaQuery.taskAssignee(query.getAssignee());
        }
        if (StringUtils.hasText(query.getCandidateUser())) {
            camundaQuery.taskCandidateUser(query.getCandidateUser());
        }
        if (StringUtils.hasText(query.getCandidateGroup())) {
            camundaQuery.taskCandidateGroup(query.getCandidateGroup());
        }
        if (StringUtils.hasText(query.getProcessInstanceId())) {
            camundaQuery.processInstanceId(query.getProcessInstanceId());
        }
        if (StringUtils.hasText(query.getProcessDefinitionKey())) {
            camundaQuery.processDefinitionKey(query.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(query.getBusinessKey())) {
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

        camundaQuery.orderByTaskCreateTime().desc();

        long total = camundaQuery.count();
        int firstResult = (query.getPageNo() - 1) * query.getPageSize();
        List<Task> list = camundaQuery.listPage(firstResult, query.getPageSize());
        List<TaskInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, query.getPageSize(), query.getPageNo());
    }

    @Override
    public TaskInfo get(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            HistoricTaskInstance historic = historyService.createHistoricTaskInstanceQuery()
                    .taskId(taskId).singleResult();
            if (historic == null) {
                throw new IllegalStateException("任务不存在: " + taskId);
            }
            return toHistoricInfo(historic);
        }
        return toInfo(task);
    }

    @Override
    public void claim(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    @Override
    public void unclaim(String taskId) {
        taskService.setAssignee(taskId, null);
    }

    @Override
    public void assign(String taskId, String userId) {
        taskService.setAssignee(taskId, userId);
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, String comment, String assignee) {
        if (StringUtils.hasText(assignee)) {
            taskService.setAssignee(taskId, assignee);
        }
        if (StringUtils.hasText(comment)) {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                taskService.createComment(taskId, task.getProcessInstanceId(), comment);
            }
        }
        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
    }

    @Override
    public void delegate(String taskId, String userId) {
        taskService.delegateTask(taskId, userId);
    }

    @Override
    public void resolve(String taskId, Map<String, Object> variables) {
        if (variables != null && !variables.isEmpty()) {
            taskService.resolveTask(taskId, variables);
        } else {
            taskService.resolveTask(taskId);
        }
    }

    @Override
    public List<CommentInfo> comments(String taskId) {
        return taskService.getTaskComments(taskId).stream().map(this::toCommentInfo).toList();
    }

    @Override
    public void addComment(String taskId, String processInstanceId, String message) {
        taskService.createComment(taskId, processInstanceId, message);
    }

    @Override
    public List<TaskInfo> getActiveTasks(String processInstanceId) {
        return taskService.createTaskQuery()
                .active()
                .processInstanceId(processInstanceId)
                .list().stream().map(this::toInfo).toList();
    }

    private TaskInfo toInfo(Task task) {
        ProcessInstance pi = null;
        ProcessDefinition pd = null;
        if (task.getProcessInstanceId() != null) {
            pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
        }
        if (task.getProcessDefinitionId() != null) {
            pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
        }

        TaskInfo.TaskInfoBuilder builder = TaskInfo.builder()
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
            builder.createTime(toLocalDateTime(task.getCreateTime()));
        }
        if (task.getDueDate() != null) {
            builder.dueDate(toLocalDateTime(task.getDueDate()));
        }
        if (task.getFollowUpDate() != null) {
            builder.followUpDate(toLocalDateTime(task.getFollowUpDate()));
        }
        if (task.getPriority() > 0) {
            builder.priority(task.getPriority());
        }
        if (pi != null) {
            builder.businessKey(pi.getBusinessKey());
        }
        if (pd != null) {
            builder.processDefinitionKey(pd.getKey())
                    .processDefinitionName(pd.getName());
        }
        return builder.build();
    }

    private TaskInfo toHistoricInfo(HistoricTaskInstance task) {
        TaskInfo.TaskInfoBuilder builder = TaskInfo.builder()
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
            builder.createTime(toLocalDateTime(task.getStartTime()));
        }
        if (task.getDueDate() != null) {
            builder.dueDate(toLocalDateTime(task.getDueDate()));
        }
        if (task.getPriority() > 0) {
            builder.priority(task.getPriority());
        }
        return builder.build();
    }

    private CommentInfo toCommentInfo(Comment comment) {
        return CommentInfo.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .processInstanceId(comment.getProcessInstanceId())
                .userId(comment.getUserId())
                .message(comment.getFullMessage())
                .time(comment.getTime() != null ? toLocalDateTime(comment.getTime()) : null)
                .build();
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
