package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "流程实例")
public class ProcessInstanceVO implements Serializable {

    @Schema(name = "流程实例ID")
    private String id;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "流程定义ID")
    private String processDefinitionId;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "流程定义名称")
    private String processDefinitionName;

    @Schema(name = "流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(name = "启动时间")
    private LocalDateTime startTime;

    @Schema(name = "启动用户ID")
    private String startUserId;

    @Schema(name = "是否挂起")
    private Boolean suspended;

    @Schema(name = "是否已结束")
    private Boolean ended;

    @Schema(name = "当前活动节点ID列表")
    private List<String> currentActivityIds;

    @Schema(name = "当前活动节点名称列表")
    private List<String> currentActivityNames;

    @Schema(name = "租户ID")
    private String tenantId;
}
