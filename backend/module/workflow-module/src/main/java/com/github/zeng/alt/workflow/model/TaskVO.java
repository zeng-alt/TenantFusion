package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户任务视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "用户任务")
public class TaskVO implements Serializable {

    @Schema(name = "任务ID")
    private String id;

    @Schema(name = "任务名称")
    private String name;

    @Schema(name = "任务描述")
    private String description;

    @Schema(name = "任务定义Key")
    private String taskDefinitionKey;

    @Schema(name = "办理人")
    private String assignee;

    @Schema(name = "任务所属人")
    private String owner;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "到期时间")
    private LocalDateTime dueDate;

    @Schema(name = "跟进时间")
    private LocalDateTime followUpDate;

    @Schema(name = "优先级（0-100，数值越大优先级越高）")
    private Integer priority;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "执行流ID")
    private String executionId;

    @Schema(name = "流程定义ID")
    private String processDefinitionId;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "流程定义名称")
    private String processDefinitionName;

    @Schema(name = "发起人用户ID")
    private String initiator;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "是否挂起")
    private Boolean suspended;

    @Schema(name = "任务本地变量")
    private Map<String, Object> taskLocalVariables;

    @Schema(name = "流程变量")
    private Map<String, Object> processVariables;

    @Schema(name = "租户ID")
    private String tenantId;
}
