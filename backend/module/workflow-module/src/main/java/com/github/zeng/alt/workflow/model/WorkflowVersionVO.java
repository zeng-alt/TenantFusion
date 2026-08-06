package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程版本视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "流程版本")
public class WorkflowVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "版本ID")
    private Long versionId;

    @Schema(name = "流程ID")
    private Long workflowId;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "版本状态")
    private WorkflowVersionStatus status;

    @Schema(name = "是否当前生效版本")
    private Boolean current;

    @Schema(name = "BPMN XML 内容")
    private String bpmnXml;

    @Schema(name = "Camunda 部署ID")
    private String deploymentId;

    @Schema(name = "Camunda 流程定义ID")
    private String processDefinitionId;

    @Schema(name = "发布时间")
    private LocalDateTime publishedDate;

    @Schema(name = "发布人")
    private String publishedBy;

    @Schema(name = "版本备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;
}