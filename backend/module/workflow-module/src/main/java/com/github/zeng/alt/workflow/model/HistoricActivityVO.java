package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史活动节点视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "历史活动节点")
public class HistoricActivityVO implements Serializable {

    @Schema(name = "活动实例ID")
    private String id;

    @Schema(name = "活动名称")
    private String activityName;

    @Schema(name = "活动类型")
    private String activityType;

    @Schema(name = "开始时间")
    private LocalDateTime startTime;

    @Schema(name = "结束时间")
    private LocalDateTime endTime;

    @Schema(name = "持续时长（毫秒）")
    private Long durationInMillis;

    @Schema(name = "办理人")
    private String assignee;

    @Schema(name = "关联任务ID")
    private String taskId;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "流程定义ID")
    private String processDefinitionId;
}
