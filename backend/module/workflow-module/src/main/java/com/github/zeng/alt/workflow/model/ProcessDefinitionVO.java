package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "流程定义")
public class ProcessDefinitionVO implements Serializable {

    @Schema(name = "流程定义ID")
    private String id;

    @Schema(name = "流程定义Key")
    private String key;

    @Schema(name = "流程定义名称")
    private String name;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "描述")
    private String description;

    @Schema(name = "分类")
    private String category;

    @Schema(name = "是否挂起")
    private Boolean suspended;

    @Schema(name = "部署ID")
    private String deploymentId;

    @Schema(name = "资源文件名（BPMN）")
    private String resourceName;

    @Schema(name = "流程图文件名（SVG/PNG）")
    private String diagramResourceName;

    @Schema(name = "租户ID")
    private String tenantId;
}
