package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.ProcessInstanceQuery;
import com.github.zeng.alt.workflow.model.ProcessInstanceVO;
import com.github.zeng.alt.workflow.model.StartProcessCmd;

import java.util.Map;

/**
 * 流程实例服务接口
 *
 * @author zengAlt
 */
public interface ProcessInstanceService {

    /**
     * 分页查询流程实例
     */
    PageRestResponse<ProcessInstanceVO> queryInstances(ProcessInstanceQuery query);

    /**
     * 获取流程实例详情
     */
    ProcessInstanceVO getInstance(String id);

    /**
     * 启动流程
     */
    ProcessInstanceVO startProcess(StartProcessCmd cmd);

    /**
     * 挂起流程实例
     */
    void suspendInstance(String id);

    /**
     * 激活流程实例
     */
    void activateInstance(String id);

    /**
     * 删除/终止流程实例
     */
    void deleteInstance(String id, String reason);

    /**
     * 获取流程变量
     */
    Map<String, Object> getVariables(String id);

    /**
     * 设置流程变量
     */
    void setVariables(String id, Map<String, Object> variables);
}
