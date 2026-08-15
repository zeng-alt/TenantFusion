package com.github.zeng.alt.camunda.engine.embedded.correlation;

import com.github.zeng.alt.camunda.engine.api.correlation.SendSignalCmd;
import com.github.zeng.alt.camunda.engine.api.correlation.SignalApi;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 嵌入式信号发送实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedSignalApi implements SignalApi {

    private final RuntimeService runtimeService;

    public EmbeddedSignalApi(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void send(SendSignalCmd cmd) {
        if (StringUtils.hasText(cmd.getExecutionId())) {
            runtimeService.signalEventReceived(cmd.getSignalName(), cmd.getExecutionId(), cmd.getVariables());
        } else if (cmd.getVariables() != null && !cmd.getVariables().isEmpty()) {
            runtimeService.signalEventReceived(cmd.getSignalName(), cmd.getVariables());
        } else {
            runtimeService.signalEventReceived(cmd.getSignalName());
        }
    }
}
