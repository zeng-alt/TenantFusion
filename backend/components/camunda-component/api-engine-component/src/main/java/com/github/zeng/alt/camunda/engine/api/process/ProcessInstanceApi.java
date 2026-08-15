package com.github.zeng.alt.camunda.engine.api.process;

import com.github.zeng.alt.api.rest.PageRestResponse;

import java.util.Map;

/**
 * 流程实例运行时 API
 * <p>
 * 对应嵌入式 RuntimeService 的流程实例查询/操作能力。
 *
 * @author zengAlt
 */
public interface ProcessInstanceApi {

    /**
     * 分页查询流程实例
     */
    PageRestResponse<ProcessInstanceInfo> query(ProcessInstanceQuery query);

    /**
     * 查询流程实例详情
     */
    ProcessInstanceInfo get(String processInstanceId);

    /**
     * 挂起流程实例
     */
    void suspend(String processInstanceId);

    /**
     * 激活流程实例
     */
    void activate(String processInstanceId);

    /**
     * 删除流程实例
     */
    void delete(String processInstanceId, String reason);

    /**
     * 获取流程变量
     */
    Map<String, Object> getVariables(String processInstanceId);

    /**
     * 设置流程变量
     */
    void setVariables(String processInstanceId, Map<String, Object> variables);
}
