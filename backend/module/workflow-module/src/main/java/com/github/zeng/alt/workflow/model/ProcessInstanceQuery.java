package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程实例查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "流程实例查询参数")
public class ProcessInstanceQuery extends WorkflowPageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "是否挂起")
    private Boolean suspended;

    @Schema(name = "启动用户ID")
    private String startUserId;

    @Schema(name = "租户ID")
    private String tenantId;
}
