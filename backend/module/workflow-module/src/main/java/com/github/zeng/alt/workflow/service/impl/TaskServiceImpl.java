package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.history.HistoryApi;
import com.github.zeng.alt.camunda.engine.api.task.CommentInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.camunda.engine.api.task.TaskQuery;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.workflow.model.*;
import com.github.zeng.alt.workflow.service.TaskFormResolver;
import com.github.zeng.alt.workflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final TaskApi taskApi;
    private final HistoryApi historyApi;
    private final TaskFormResolver taskFormResolver;
    private final AuthHelper authHelper;

    @Override
    public PageRestResponse<TaskVO> queryTasks(com.github.zeng.alt.workflow.model.TaskQuery query) {
        PageRestResponse<TaskInfo> page = taskApi.query(TaskQuery.builder()
                .name(query.getName())
                .processDefinitionName(query.getProcessDefinitionName())
                .userId(query.getUserId())
                .initiator(query.getInitiator())
                .taskDefinitionKey(query.getTaskDefinitionKey())
                .assignee(query.getAssignee())
                .candidateUser(query.getCandidateUser())
                .candidateGroup(query.getCandidateGroup())
                .processInstanceId(query.getProcessInstanceId())
                .processDefinitionKey(query.getProcessDefinitionKey())
                .businessKey(query.getBusinessKey())
                .suspended(query.getSuspended())
                .unassigned(query.getUnassigned())
                .tenantId(query.getTenantId())
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .build());

        List<TaskVO> vos = page.getData().getPageData().stream().map(this::toVO).toList();
        Map<String, String> initiators = loadInitiatorMap(
                vos.stream().map(TaskVO::getProcessInstanceId).filter(Objects::nonNull).distinct().toList());
        vos.forEach(vo -> vo.setInitiator(initiators.get(vo.getProcessInstanceId())));

        return PageRestResponse.of(vos, page.getData().getTotal(), query.getPageSize(), query.getPageNo());
    }

    @Override
    public TaskVO getTask(String id) {
        TaskInfo task = taskApi.get(id);
        TaskVO vo = toVO(task);
        if (vo.getProcessInstanceId() != null) {
            vo.setInitiator(loadInitiatorMap(List.of(vo.getProcessInstanceId())).get(vo.getProcessInstanceId()));
        }
        return vo;
    }

    @Override
    public List<TaskFormDefinitionVO> getTaskForms(String id) {
        return taskFormResolver.resolveByTaskId(id);
    }

    @Override
    public void claimTask(String taskId, String userId) {
        taskApi.claim(taskId, userId);
        log.info("签收任务: " + taskId + ", 用户: " + userId);
    }

    @Override
    public void unclaimTask(String taskId) {
        TaskInfo task = taskApi.get(taskId);
        String currentUser = UserContextHolder.getUsername();
        if (!Objects.equals(task.getAssignee(), currentUser) && !authHelper.isAdmin() && !authHelper.isSuperAdmin()) {
            throw new BaseException("仅任务认领人、管理员或超级用户可取消认领");
        }
        taskApi.unclaim(taskId);
        log.info("取消签收任务: " + taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(CompleteTaskCmd cmd) {
        taskApi.complete(cmd.getTaskId(), cmd.getVariables(), cmd.getComment(), UserContextHolder.getUsername());
        log.info("完成任务: " + cmd.getTaskId());
    }

    @Override
    public void delegateTask(String taskId, String userId) {
        taskApi.delegate(taskId, userId);
        log.info("委派任务: " + taskId + " -> " + userId);
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables) {
        taskApi.resolve(taskId, variables);
        log.info("解决委派任务: " + taskId);
    }

    @Override
    public void assignTask(String taskId, String userId) {
        taskApi.assign(taskId, userId);
        log.info("分配任务: " + taskId + " -> " + userId);
    }

    @Override
    public PageRestResponse<TaskCommentVO> getComments(String taskId, int pageNum, int pageSize) {
        List<CommentInfo> comments = taskApi.comments(taskId);
        int fromIndex = Math.min((pageNum - 1) * pageSize, comments.size());
        int toIndex = Math.min(fromIndex + pageSize, comments.size());
        List<TaskCommentVO> vos = comments.subList(fromIndex, toIndex)
                .stream().map(this::toCommentVO).toList();

        return PageRestResponse.of(vos, (long) comments.size(), pageSize, pageNum);
    }

    @Override
    public void addComment(String taskId, String processInstanceId, String message) {
        taskApi.addComment(taskId, processInstanceId, message);
        log.info("添加批注: task=" + taskId + ", msg=" + message);
    }

    private TaskVO toVO(TaskInfo task) {
        if (task == null) {
            return null;
        }
        return TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .processInstanceId(task.getProcessInstanceId())
                .executionId(task.getExecutionId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(task.getProcessDefinitionKey())
                .processDefinitionName(task.getProcessDefinitionName())
                .businessKey(task.getBusinessKey())
                .suspended(task.getSuspended())
                .tenantId(task.getTenantId())
                .build();
    }

    private TaskCommentVO toCommentVO(CommentInfo comment) {
        return TaskCommentVO.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .processInstanceId(comment.getProcessInstanceId())
                .userId(comment.getUserId())
                .message(comment.getMessage())
                .time(comment.getTime())
                .build();
    }

    private Map<String, String> loadInitiatorMap(List<String> processInstanceIds) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (String id : processInstanceIds) {
            try {
                String initiator = historyApi.getProcessInstance(id).getInitiator();
                if (initiator != null) {
                    map.put(id, initiator);
                }
            } catch (Exception e) {
                log.debug("加载发起人失败: " + id, e);
            }
        }
        return map;
    }
}
