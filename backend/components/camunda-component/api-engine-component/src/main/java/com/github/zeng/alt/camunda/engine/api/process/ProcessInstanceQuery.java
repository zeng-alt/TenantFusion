package com.github.zeng.alt.camunda.engine.api.process;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程实例查询
 *
 * @author zengAlt
 */
@Data
@Builder
public class ProcessInstanceQuery implements Serializable {

    private String processDefinitionKey;
    private String businessKey;
    private Boolean suspended;
    private String tenantId;
    @Builder.Default
    private Integer pageNo = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
