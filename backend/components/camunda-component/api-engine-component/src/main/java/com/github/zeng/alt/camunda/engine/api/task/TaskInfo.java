package com.github.zeng.alt.camunda.engine.api.task;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户任务信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class TaskInfo implements Serializable {

    private String id;
    private String name;
    private String description;
    private String taskDefinitionKey;
    private String assignee;
    private String owner;
    private LocalDateTime createTime;
    private LocalDateTime dueDate;
    private LocalDateTime followUpDate;
    private Integer priority;
    private String processInstanceId;
    private String executionId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private Boolean suspended;
    private String tenantId;
}
