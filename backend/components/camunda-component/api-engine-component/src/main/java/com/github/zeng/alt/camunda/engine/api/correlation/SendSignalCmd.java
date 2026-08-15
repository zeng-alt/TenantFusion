package com.github.zeng.alt.camunda.engine.api.correlation;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 信号发送命令
 *
 * @author zengAlt
 */
@Data
@Builder
public class SendSignalCmd implements Serializable {

    /**
     * 信号名称
     */
    private String signalName;

    /**
     * 执行流ID
     */
    private String executionId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 租户ID
     */
    private String tenantId;
}
