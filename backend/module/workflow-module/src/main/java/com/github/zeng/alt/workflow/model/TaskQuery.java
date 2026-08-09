package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 任务查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "任务查询参数")
public class TaskQuery extends WorkflowPageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "任务名称（模糊匹配）")
    private String name;

    @Schema(name = "流程定义名称（模糊匹配）")
    private String processDefinitionName;

    @Schema(name = "待办用户ID（办理人或候选人）")
    private String userId;

    @Schema(name = "发起人用户ID")
    private String initiator;

    @Schema(name = "任务定义Key")
    private String taskDefinitionKey;

    @Schema(name = "办理人")
    private String assignee;

    @Schema(name = "候选用户")
    private String candidateUser;

    @Schema(name = "候选组")
    private String candidateGroup;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "是否挂起")
    private Boolean suspended;

    @Schema(name = "是否仅查询未办理的任务")
    private Boolean unassigned;

    @Schema(name = "租户ID")
    private String tenantId;
}
