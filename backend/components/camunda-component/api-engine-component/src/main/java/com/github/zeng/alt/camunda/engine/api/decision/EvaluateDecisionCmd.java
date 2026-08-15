package com.github.zeng.alt.camunda.engine.api.decision;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 决策求值命令
 *
 * @author zengAlt
 */
@Data
@Builder
public class EvaluateDecisionCmd implements Serializable {

    /**
     * 决策定义Key
     */
    private String decisionDefinitionKey;

    /**
     * 决策定义ID
     */
    private String decisionDefinitionId;

    /**
     * 输入变量
     */
    private Map<String, Object> variables;

    /**
     * 租户ID
     */
    private String tenantId;
}
