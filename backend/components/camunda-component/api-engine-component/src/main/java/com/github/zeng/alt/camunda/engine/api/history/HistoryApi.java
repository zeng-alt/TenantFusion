package com.github.zeng.alt.camunda.engine.api.history;

import com.github.zeng.alt.api.rest.PageRestResponse;

import java.util.List;

/**
 * 历史数据 API
 * <p>
 * 覆盖嵌入式 HistoryService 的查询能力。发起人字段由各实现统一推导。
 *
 * @author zengAlt
 */
public interface HistoryApi {

    /**
     * 分页查询历史流程实例
     */
    PageRestResponse<HistoricProcessInstanceInfo> queryProcessInstances(HistoricProcessInstanceQuery query);

    /**
     * 查询历史流程实例详情
     */
    HistoricProcessInstanceInfo getProcessInstance(String processInstanceId);

    /**
     * 分页查询历史任务
     */
    PageRestResponse<HistoricTaskInfo> queryTasks(String assignee, String processInstanceId, Boolean finished,
                                                  int pageNo, int pageSize);

    /**
     * 查询流程实例的历史活动（按开始时间正序）
     */
    List<HistoricActivityInfo> activities(String processInstanceId);

    /**
     * 分页查询历史变量
     */
    PageRestResponse<HistoricVariableInfo> queryVariables(String processInstanceId, String variableName,
                                                          int pageNo, int pageSize);

    /**
     * 查询流程实例的全部历史变量
     */
    List<HistoricVariableInfo> variables(String processInstanceId);
}
