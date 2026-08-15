package com.github.zeng.alt.camunda.engine.remote.correlation;

import com.github.zeng.alt.camunda.engine.api.correlation.CorrelateMessageCmd;
import com.github.zeng.alt.camunda.engine.api.correlation.CorrelationApi;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.MessageApiClient;
import org.camunda.community.rest.client.model.CorrelationMessageDto;
import org.springframework.stereotype.Service;

/**
 * 远程消息关联实现
 *
 * @author zengAlt
 */
@Service
public class RemoteCorrelationApi implements CorrelationApi {

    private final MessageApiClient messageApiClient;

    public RemoteCorrelationApi(MessageApiClient messageApiClient) {
        this.messageApiClient = messageApiClient;
    }

    @Override
    public void correlateMessage(CorrelateMessageCmd cmd) {
        CorrelationMessageDto dto = new CorrelationMessageDto();
        dto.setMessageName(cmd.getMessageName());
        dto.setBusinessKey(cmd.getBusinessKey());
        dto.setProcessInstanceId(cmd.getProcessInstanceId());
        dto.setTenantId(cmd.getTenantId());
        dto.setProcessVariables(RemoteSupport.toVariableMap(cmd.getVariables()));
        dto.setAll(false);
        messageApiClient.deliverMessage(dto);
    }
}
