package com.github.zeng.alt.camunda.engine.api.history;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史活动信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class HistoricActivityInfo implements Serializable {

    private String id;
    private String activityId;
    private String activityName;
    private String activityType;
    private String assignee;
    private String processInstanceId;
    private String executionId;
    private String taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInMillis;
}
