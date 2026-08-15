package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.workflow.model.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.workflow.model.StartByMessageAtElementCmd;
import com.github.zeng.alt.workflow.model.StartByMessageCmd;
import com.github.zeng.alt.workflow.model.StartByProcessDefinitionCmd;

/**
 * 流程启动服务接口
 *
 * @author zengAlt
 */
public interface WorkflowStarterService {

    /**
     * 按流程定义Key启动流程
     */
    ProcessInformation startByProcessDefinition(StartByProcessDefinitionCmd cmd);

    /**
     * 按消息名称启动流程
     */
    ProcessInformation startByMessage(StartByMessageCmd cmd);

    /**
     * 按流程定义Key在指定节点启动流程
     */
    ProcessInformation startByDefinitionAtActivity(StartByDefinitionAtElementCmd cmd);

    /**
     * 按消息名称在指定节点启动流程
     */
    ProcessInformation startByMessageAtActivity(StartByMessageAtElementCmd cmd);
}
