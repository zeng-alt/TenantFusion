package com.github.zeng.alt.camunda.engine.remote.decision;

import com.github.zeng.alt.camunda.engine.api.decision.DecisionEvaluationResult;
import com.github.zeng.alt.camunda.engine.api.decision.EvaluateDecisionApi;
import com.github.zeng.alt.camunda.engine.api.decision.EvaluateDecisionCmd;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.DecisionDefinitionApiClient;
import org.camunda.community.rest.client.model.EvaluateDecisionDto;
import org.camunda.community.rest.client.model.VariableValueDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程决策求值实现
 *
 * @author zengAlt
 */
@Service
public class RemoteEvaluateDecisionApi implements EvaluateDecisionApi {

    private final DecisionDefinitionApiClient decisionDefinitionApiClient;

    public RemoteEvaluateDecisionApi(DecisionDefinitionApiClient decisionDefinitionApiClient) {
        this.decisionDefinitionApiClient = decisionDefinitionApiClient;
    }

    @Override
    public DecisionEvaluationResult evaluateDecision(EvaluateDecisionCmd cmd) {
        EvaluateDecisionDto dto = new EvaluateDecisionDto();
        dto.setVariables(RemoteSupport.toVariableMap(cmd.getVariables()));
        List<Map<String, VariableValueDto>> outputs;
        if (StringUtils.hasText(cmd.getDecisionDefinitionId())) {
            outputs = decisionDefinitionApiClient.evaluateDecisionById(cmd.getDecisionDefinitionId(), dto).getBody();
        } else {
            outputs = decisionDefinitionApiClient.evaluateDecisionByKey(cmd.getDecisionDefinitionKey(), dto).getBody();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (outputs != null) {
            for (Map<String, VariableValueDto> output : outputs) {
                Map<String, Object> row = new LinkedHashMap<>();
                output.forEach((k, v) -> row.put(k, v == null ? null : v.getValue()));
                result.add(row);
            }
        }
        return DecisionEvaluationResult.builder().outputs(result).build();
    }
}
