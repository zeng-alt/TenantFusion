package com.github.zeng.alt.camunda.engine.api.task;

import com.github.zeng.alt.api.rest.PageRestResponse;

import java.util.List;
import java.util.Map;

/**
 * 用户任务 API
 * <p>
 * 覆盖嵌入式 TaskService 的任务查询与办理能力。
 *
 * @author zengAlt
 */
public interface TaskApi {

    /**
     * 分页查询任务
     */
    PageRestResponse<TaskInfo> query(TaskQuery query);

    /**
     * 查询任务详情（任务不存在时回退历史）
     */
    TaskInfo get(String taskId);

    /**
     * 认领任务
     */
    void claim(String taskId, String userId);

    /**
     * 取消认领
     */
    void unclaim(String taskId);

    /**
     * 改派任务
     */
    void assign(String taskId, String userId);

    /**
     * 完成任务（可携带变量与批注，assignee 为办理人）
     */
    void complete(String taskId, Map<String, Object> variables, String comment, String assignee);

    /**
     * 委派任务
     */
    void delegate(String taskId, String userId);

    /**
     * 解决委派任务
     */
    void resolve(String taskId, Map<String, Object> variables);

    /**
     * 查询任务批注
     */
    List<CommentInfo> comments(String taskId);

    /**
     * 添加批注
     */
    void addComment(String taskId, String processInstanceId, String message);

    /**
     * 查询流程实例当前活动任务
     */
    List<TaskInfo> getActiveTasks(String processInstanceId);
}
