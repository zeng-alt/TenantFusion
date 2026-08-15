package com.github.zeng.alt.camunda.engine.api.decision;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 决策求值结果
 *
 * @author zengAlt
 */
@Data
@Builder
public class DecisionEvaluationResult implements Serializable {

    /**
     * 输出结果（每条匹配规则一行，key=输出名称 value=值）
     */
    private List<Map<String, Object>> outputs;
}
