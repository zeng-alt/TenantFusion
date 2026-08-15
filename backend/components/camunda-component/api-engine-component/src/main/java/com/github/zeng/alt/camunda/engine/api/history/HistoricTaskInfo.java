package com.github.zeng.alt.camunda.engine.api.history;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史任务信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class HistoricTaskInfo implements Serializable {

    private String id;
    private String name;
    private String description;
    private String taskDefinitionKey;
    private String assignee;
    private String owner;
    private String processInstanceId;
    private String processDefinitionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime dueDate;
    private Integer priority;
}
