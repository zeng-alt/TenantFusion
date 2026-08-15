package com.github.zeng.alt.camunda.engine.remote.task;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.task.CommentInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskQuery;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.HistoryApiClient;
import org.camunda.community.rest.client.api.TaskApiClient;
import org.camunda.community.rest.client.model.CommentDto;
import org.camunda.community.rest.client.model.CompleteTaskDto;
import org.camunda.community.rest.client.model.HistoricProcessInstanceDto;
import org.camunda.community.rest.client.model.HistoricProcessInstanceQueryDto;
import org.camunda.community.rest.client.model.TaskQueryDto;
import org.camunda.community.rest.client.model.TaskQueryDtoSortingInner;
import org.camunda.community.rest.client.model.TaskWithAttachmentAndCommentDto;
import org.camunda.community.rest.client.model.UserIdDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 远程用户任务实现
 *
 * @author zengAlt
 */
@Service
public class RemoteTaskApi implements TaskApi {

    private final TaskApiClient taskApiClient;
    private final HistoryApiClient historyApiClient;

    public RemoteTaskApi(TaskApiClient taskApiClient, HistoryApiClient historyApiClient) {
        this.taskApiClient = taskApiClient;
        this.historyApiClient = historyApiClient;
    }

    @Override
    public PageRestResponse<TaskInfo> query(TaskQuery query) {
        TaskQueryDto dto = new TaskQueryDto();
        if (StringUtils.hasText(query.getName())) {
            dto.setNameLike("%" + query.getName() + "%");
        }
        if (StringUtils.hasText(query.getProcessDefinitionName())) {
            dto.setProcessDefinitionNameLike("%" + query.getProcessDefinitionName() + "%");
        }
        if (StringUtils.hasText(query.getUserId())) {
            dto.setAssignee(query.getUserId());
        }
        if (StringUtils.hasText(query.getInitiator())) {
            HistoricProcessInstanceQueryDto hpiQuery = new HistoricProcessInstanceQueryDto();
            hpiQuery.setStartedBy(query.getInitiator());
            List<String> ids = historyApiClient
                    .queryHistoricProcessInstances(0, Integer.MAX_VALUE, hpiQuery).getBody().stream()
                    .map(HistoricProcessInstanceDto::getId).toList();
            if (ids.isEmpty()) {
                return PageRestResponse.of(List.<TaskInfo>of(), 0L, query.getPageSize(), query.getPageNo());
            }
            dto.setProcessInstanceIdIn(ids);
        }
        if (StringUtils.hasText(query.getTaskDefinitionKey())) {
            dto.setTaskDefinitionKey(query.getTaskDefinitionKey());
        }
        if (StringUtils.hasText(query.getAssignee())) {
            dto.setAssignee(query.getAssignee());
        }
        if (StringUtils.hasText(query.getCandidateUser())) {
            dto.setCandidateUser(query.getCandidateUser());
        }
        if (StringUtils.hasText(query.getCandidateGroup())) {
            dto.setCandidateGroup(query.getCandidateGroup());
        }
        if (StringUtils.hasText(query.getProcessInstanceId())) {
            dto.setProcessInstanceId(query.getProcessInstanceId());
        }
        if (StringUtils.hasText(query.getProcessDefinitionKey())) {
            dto.setProcessDefinitionKey(query.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(query.getBusinessKey())) {
            dto.setProcessInstanceBusinessKey(query.getBusinessKey());
        }
        if (query.getSuspended() != null && query.getSuspended()) {
            dto.setSuspended(true);
        } else if (query.getSuspended() != null) {
            dto.setActive(true);
        }
        if (query.getUnassigned() != null && query.getUnassigned()) {
            dto.setUnassigned(true);
        }
        if (StringUtils.hasText(query.getTenantId())) {
            dto.setTenantIdIn(List.of(query.getTenantId()));
        }
        TaskQueryDtoSortingInner sorting = new TaskQueryDtoSortingInner();
        sorting.setSortBy(TaskQueryDtoSortingInner.SortByEnum.CREATED);
        sorting.setSortOrder(TaskQueryDtoSortingInner.SortOrderEnum.DESC);
        dto.setSorting(List.of(sorting));

        long total = taskApiClient.queryTasksCount(dto).getBody().getCount();
        int firstResult = (query.getPageNo() - 1) * query.getPageSize();
        List<TaskWithAttachmentAndCommentDto> list = taskApiClient
                .queryTasks(firstResult, query.getPageSize(), dto).getBody();
        List<TaskInfo> vos = list.stream().map(this::toInfo).toList();
        return PageRestResponse.of(vos, total, query.getPageSize(), query.getPageNo());
    }

    @Override
    public TaskInfo get(String taskId) {
        TaskWithAttachmentAndCommentDto dto = taskApiClient.getTask(taskId).getBody();
        return dto == null ? null : toInfo(dto);
    }

    @Override
    public void claim(String taskId, String userId) {
        UserIdDto dto = new UserIdDto();
        dto.setUserId(userId);
        taskApiClient.claim(taskId, dto);
    }

    @Override
    public void unclaim(String taskId) {
        taskApiClient.unclaim(taskId);
    }

    @Override
    public void assign(String taskId, String userId) {
        UserIdDto dto = new UserIdDto();
        dto.setUserId(userId);
        taskApiClient.setAssignee(taskId, dto);
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, String comment, String assignee) {
        if (StringUtils.hasText(assignee)) {
            UserIdDto userDto = new UserIdDto();
            userDto.setUserId(assignee);
            taskApiClient.setAssignee(taskId, userDto);
        }
        if (StringUtils.hasText(comment)) {
            CommentDto commentDto = new CommentDto();
            commentDto.setMessage(comment);
            taskApiClient.createComment(taskId, commentDto);
        }
        CompleteTaskDto dto = new CompleteTaskDto();
        dto.setVariables(RemoteSupport.toVariableMap(variables));
        taskApiClient.complete(taskId, dto);
    }

    @Override
    public void delegate(String taskId, String userId) {
        UserIdDto dto = new UserIdDto();
        dto.setUserId(userId);
        taskApiClient.delegateTask(taskId, dto);
    }

    @Override
    public void resolve(String taskId, Map<String, Object> variables) {
        CompleteTaskDto dto = new CompleteTaskDto();
        dto.setVariables(RemoteSupport.toVariableMap(variables));
        taskApiClient.resolve(taskId, dto);
    }

    @Override
    public List<CommentInfo> comments(String taskId) {
        return taskApiClient.getComments(taskId).getBody().stream().map(this::toCommentInfo).toList();
    }

    @Override
    public void addComment(String taskId, String processInstanceId, String message) {
        CommentDto dto = new CommentDto();
        dto.setMessage(message);
        dto.setProcessInstanceId(processInstanceId);
        taskApiClient.createComment(taskId, dto);
    }

    @Override
    public List<TaskInfo> getActiveTasks(String processInstanceId) {
        TaskQueryDto dto = new TaskQueryDto();
        dto.setProcessInstanceId(processInstanceId);
        dto.setActive(true);
        return taskApiClient.queryTasks(0, Integer.MAX_VALUE, dto).getBody().stream()
                .map(this::toInfo).toList();
    }

    private TaskInfo toInfo(TaskWithAttachmentAndCommentDto task) {
        return TaskInfo.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(RemoteSupport.toLocalDateTime(task.getCreated()))
                .dueDate(RemoteSupport.toLocalDateTime(task.getDue()))
                .followUpDate(RemoteSupport.toLocalDateTime(task.getFollowUp()))
                .priority(task.getPriority())
                .processInstanceId(task.getProcessInstanceId())
                .executionId(task.getExecutionId())
                .processDefinitionId(task.getProcessDefinitionId())
                .tenantId(task.getTenantId())
                .suspended(task.getSuspended())
                .build();
    }

    private CommentInfo toCommentInfo(CommentDto comment) {
        return CommentInfo.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .processInstanceId(comment.getProcessInstanceId())
                .userId(comment.getUserId())
                .message(comment.getMessage())
                .time(RemoteSupport.toLocalDateTime(comment.getTime()))
                .build();
    }
}
