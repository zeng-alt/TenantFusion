package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 历史流程实例视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "历史流程实例")
public class HistoricProcessInstanceVO implements Serializable {

    @Schema(name = "流程实例ID")
    private String id;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "流程定义名称")
    private String processDefinitionName;

    @Schema(name = "流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(name = "开始时间")
    private LocalDateTime startTime;

    @Schema(name = "结束时间")
    private LocalDateTime endTime;

    @Schema(name = "持续时长（毫秒）")
    private Long durationInMillis;

    @Schema(name = "启动用户ID")
    private String startUserId;

    @Schema(name = "结束状态：completed-正常完成，deleted-已删除，suspended-已挂起")
    private String state;

    @Schema(name = "删除原因")
    private String deleteReason;

    @Schema(name = "租户ID")
    private String tenantId;
}
