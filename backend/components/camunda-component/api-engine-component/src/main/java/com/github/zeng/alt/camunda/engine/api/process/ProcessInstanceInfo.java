package com.github.zeng.alt.camunda.engine.api.process;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程实例信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class ProcessInstanceInfo implements Serializable {

    private String id;
    private String businessKey;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
    private LocalDateTime startTime;
    private Boolean suspended;
    private Boolean ended;
    private String tenantId;
}
