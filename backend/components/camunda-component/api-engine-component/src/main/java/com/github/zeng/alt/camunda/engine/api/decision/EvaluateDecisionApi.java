package com.github.zeng.alt.camunda.engine.api.decision;

/**
 * 决策求值 API
 * <p>
 * 参考 dev.bpm-crafters.process-engine-api 的 EvaluateDecisionApi 设计。
 *
 * @author zengAlt
 */
public interface EvaluateDecisionApi {

    /**
     * 求值决策表
     */
    DecisionEvaluationResult evaluateDecision(EvaluateDecisionCmd cmd);
}
