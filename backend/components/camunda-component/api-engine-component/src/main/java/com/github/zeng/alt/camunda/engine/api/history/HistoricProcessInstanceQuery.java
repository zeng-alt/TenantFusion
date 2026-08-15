package com.github.zeng.alt.camunda.engine.api.history;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 历史流程实例查询
 *
 * @author zengAlt
 */
@Data
@Builder
public class HistoricProcessInstanceQuery implements Serializable {

    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    /**
     * 流程状态：running/completed/terminated/suspended
     */
    private String state;
    /**
     * 启动用户ID
     */
    private String startUserId;
    /**
     * 发起人（按 initiator 变量过滤）
     */
    private String initiator;
    private String tenantId;
    @Builder.Default
    private Integer pageNo = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
