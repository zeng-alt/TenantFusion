package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务批注视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "任务批注")
public class TaskCommentVO implements Serializable {

    @Schema(name = "批注ID")
    private String id;

    @Schema(name = "任务ID")
    private String taskId;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "批注人ID")
    private String userId;

    @Schema(name = "批注时间")
    private LocalDateTime time;

    @Schema(name = "批注内容")
    private String message;
}
