package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史变量视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "历史变量")
public class HistoricVariableVO implements Serializable {

    @Schema(name = "变量名")
    private String name;

    @Schema(name = "变量值")
    private Object value;

    @Schema(name = "变量类型")
    private String typeName;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "任务ID")
    private String taskId;

    @Schema(name = "活动实例ID")
    private String activityInstanceId;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "最后更新时间")
    private LocalDateTime lastUpdatedTime;
}
