package com.github.zeng.alt.camunda.engine.api.process;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 按流程定义Key启动流程命令
 *
 * @author zengAlt
 */
@Data
@Builder
public class StartByDefinitionCmd implements Serializable {

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 发起人（写入 initiator 变量；嵌入式实现同时设置认证用户）
     */
    private String initiator;

    /**
     * 租户ID
     */
    private String tenantId;
}
