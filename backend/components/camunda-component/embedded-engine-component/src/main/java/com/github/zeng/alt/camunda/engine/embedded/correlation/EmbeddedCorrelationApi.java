package com.github.zeng.alt.camunda.engine.embedded.correlation;

import com.github.zeng.alt.camunda.engine.api.correlation.CorrelateMessageCmd;
import com.github.zeng.alt.camunda.engine.api.correlation.CorrelationApi;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 嵌入式消息关联实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedCorrelationApi implements CorrelationApi {

    private final RuntimeService runtimeService;

    public EmbeddedCorrelationApi(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void correlateMessage(CorrelateMessageCmd cmd) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(cmd.getMessageName());
        if (StringUtils.hasText(cmd.getBusinessKey())) {
            builder.processInstanceBusinessKey(cmd.getBusinessKey());
        }
        if (StringUtils.hasText(cmd.getProcessInstanceId())) {
            builder.processInstanceId(cmd.getProcessInstanceId());
        }
        if (StringUtils.hasText(cmd.getTenantId())) {
            builder.tenantId(cmd.getTenantId());
        }
        if (cmd.getVariables() != null && !cmd.getVariables().isEmpty()) {
            builder.setVariables(cmd.getVariables());
        }
        builder.correlate();
    }
}
