package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.*;

import java.util.List;
import java.util.Map;

/**
 * 用户任务服务接口
 *
 * @author zengAlt
 */
public interface TaskService {

    /**
     * 分页查询任务
     */
    PageRestResponse<TaskVO> queryTasks(TaskQuery query);

    /**
     * 获取任务详情
     */
    TaskVO getTask(String id);

    /**
     * 获取任务表单定义
     */
    List<TaskFormDefinitionVO> getTaskForms(String id);

    /**
     * 签收/认领任务
     */
    void claimTask(String taskId, String userId);

    /**
     * 取消认领
     */
    void unclaimTask(String taskId);

    /**
     * 完成任务
     */
    void completeTask(CompleteTaskCmd cmd);

    /**
     * 委派任务给他人
     */
    void delegateTask(String taskId, String userId);

    /**
     * 被委派人处理完成，回退任务
     */
    void resolveTask(String taskId, Map<String, Object> variables);

    /**
     * 设置办理人（管理员操作）
     */
    void assignTask(String taskId, String userId);

    /**
     * 获取任务批注列表
     */
    PageRestResponse<TaskCommentVO> getComments(String taskId, int pageNum, int pageSize);

    /**
     * 添加任务批注
     */
    void addComment(String taskId, String processInstanceId, String message);
}
