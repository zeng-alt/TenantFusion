package com.github.zeng.alt.camunda.engine.api.process;

/**
 * 流程启动 API
 * <p>
 * 参考 dev.bpm-crafters.process-engine-api 的 StartProcessApi 设计，
 * 命令统一携带显式 {@code initiator}，由各实现负责记录发起人。
 *
 * @author zengAlt
 */
public interface StartProcessApi {

    /**
     * 按流程定义Key启动流程
     */
    ProcessInformation startByDefinition(StartByDefinitionCmd cmd);

    /**
     * 按消息名称启动流程
     */
    ProcessInformation startByMessage(StartByMessageCmd cmd);

    /**
     * 按流程定义Key在指定节点启动流程
     */
    ProcessInformation startByDefinitionAtElement(StartByDefinitionAtElementCmd cmd);

    /**
     * 按消息名称在指定节点启动流程
     */
    ProcessInformation startByMessageAtElement(StartByMessageAtElementCmd cmd);
}
