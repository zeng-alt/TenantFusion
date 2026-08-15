package com.github.zeng.alt.camunda.engine.remote.correlation;

import com.github.zeng.alt.camunda.engine.api.correlation.SendSignalCmd;
import com.github.zeng.alt.camunda.engine.api.correlation.SignalApi;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.SignalApiClient;
import org.camunda.community.rest.client.model.SignalDto;
import org.springframework.stereotype.Service;

/**
 * 远程信号发送实现
 *
 * @author zengAlt
 */
@Service
public class RemoteSignalApi implements SignalApi {

    private final SignalApiClient signalApiClient;

    public RemoteSignalApi(SignalApiClient signalApiClient) {
        this.signalApiClient = signalApiClient;
    }

    @Override
    public void send(SendSignalCmd cmd) {
        SignalDto dto = new SignalDto();
        dto.setName(cmd.getSignalName());
        dto.setExecutionId(cmd.getExecutionId());
        dto.setTenantId(cmd.getTenantId());
        dto.setVariables(RemoteSupport.toVariableMap(cmd.getVariables()));
        signalApiClient.throwSignal(dto);
    }
}
