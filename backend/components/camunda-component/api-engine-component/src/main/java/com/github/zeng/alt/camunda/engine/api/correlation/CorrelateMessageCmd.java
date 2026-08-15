package com.github.zeng.alt.camunda.engine.api.correlation;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 消息关联命令
 *
 * @author zengAlt
 */
@Data
@Builder
public class CorrelateMessageCmd implements Serializable {

    /**
     * 消息名称
     */
    private String messageName;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 租户ID
     */
    private String tenantId;
}
