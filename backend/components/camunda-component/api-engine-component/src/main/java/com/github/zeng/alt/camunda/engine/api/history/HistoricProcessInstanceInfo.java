package com.github.zeng.alt.camunda.engine.api.history;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史流程实例信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class HistoricProcessInstanceInfo implements Serializable {

    private String id;
    private String businessKey;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInMillis;
    /**
     * 启动用户ID（嵌入式取 START_USER_ID_；远程为引擎认证用户）
     */
    private String startUserId;
    /**
     * 发起人（统一从 initiator 变量/startUserId 推导，两端一致）
     */
    private String initiator;
    private String deleteReason;
    private String tenantId;
}
