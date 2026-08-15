package com.github.zeng.alt.camunda.engine.api.process;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 按消息名称在指定节点启动流程命令
 *
 * @author zengAlt
 */
@Data
@Builder
public class StartByMessageAtElementCmd implements Serializable {

    /**
     * 消息名称
     */
    private String messageName;

    /**
     * 目标节点ID
     */
    private String elementId;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 发起人
     */
    private String initiator;

    /**
     * 流程定义ID（可选限定）
     */
    private String processDefinitionId;

    /**
     * 租户ID
     */
    private String tenantId;
}
