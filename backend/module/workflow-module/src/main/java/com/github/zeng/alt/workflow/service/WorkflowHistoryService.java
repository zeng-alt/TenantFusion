package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.HistoricActivityVO;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceQuery;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceVO;
import com.github.zeng.alt.workflow.model.HistoricVariableVO;
import com.github.zeng.alt.workflow.model.TaskVO;

import java.util.List;

/**
 * 工作流历史服务接口
 *
 * @author zengAlt
 */
public interface WorkflowHistoryService {

    /**
     * 分页查询历史流程实例
     *
     * @param query 查询参数（流程定义Key/名称、业务键、流程状态、启动用户ID、分页）
     */
    PageRestResponse<HistoricProcessInstanceVO> queryHistoricInstances(HistoricProcessInstanceQuery query);

    /**
     * 获取历史流程实例详情
     */
    HistoricProcessInstanceVO getHistoricInstance(String id);

    /**
     * 分页查询历史任务
     */
    PageRestResponse<TaskVO> queryHistoricTasks(
            String assignee, String processInstanceId, Boolean finished,
            int pageNum, int pageSize);

    /**
     * 查询流程实例的历史活动节点（审批链路）
     */
    List<HistoricActivityVO> queryHistoricActivities(String processInstanceId);

    /**
     * 查询历史变量变更
     *
     * @param processInstanceId 流程实例ID
     * @param variableName      变量名（可选）
     * @param pageNum           页码
     * @param pageSize          每页条数
     */
    PageRestResponse<HistoricVariableVO> queryHistoricVariables(
            String processInstanceId, String variableName, int pageNum, int pageSize);
}
