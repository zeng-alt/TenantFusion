package com.github.zeng.alt.camunda.engine.embedded.decision;

import com.github.zeng.alt.camunda.engine.api.decision.DecisionEvaluationResult;
import com.github.zeng.alt.camunda.engine.api.decision.EvaluateDecisionApi;
import com.github.zeng.alt.camunda.engine.api.decision.EvaluateDecisionCmd;
import org.camunda.bpm.engine.DecisionService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入式决策求值实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedEvaluateDecisionApi implements EvaluateDecisionApi {

    private final DecisionService decisionService;

    public EmbeddedEvaluateDecisionApi(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @Override
    public DecisionEvaluationResult evaluateDecision(EvaluateDecisionCmd cmd) {
        VariableMap variables = Variables.createVariables();
        if (cmd.getVariables() != null) {
            variables.putAll(cmd.getVariables());
        }
        List<Map<String, Object>> outputs = new ArrayList<>();
        if (StringUtils.hasText(cmd.getDecisionDefinitionId())) {
            decisionService.evaluateDecisionTableById(cmd.getDecisionDefinitionId(), variables)
                    .forEach(r -> outputs.add(toOutputMap(r.getEntryMap())));
        } else {
            decisionService.evaluateDecisionTableByKey(cmd.getDecisionDefinitionKey(), variables)
                    .forEach(r -> outputs.add(toOutputMap(r.getEntryMap())));
        }
        return DecisionEvaluationResult.builder().outputs(outputs).build();
    }

    private Map<String, Object> toOutputMap(Map<String, Object> entryMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        entryMap.forEach((k, v) -> map.put(String.valueOf(k), v));
        return map;
    }
}
