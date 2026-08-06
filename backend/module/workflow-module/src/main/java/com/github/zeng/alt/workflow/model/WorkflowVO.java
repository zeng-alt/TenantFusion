package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程主表视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "流程")
public class WorkflowVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程ID")
    private Long workflowId;

    @Schema(name = "流程编码")
    private String workflowKey;

    @Schema(name = "流程名称")
    private String workflowName;

    @Schema(name = "流程描述")
    private String description;

    @Schema(name = "流程分类")
    private String category;

    @Schema(name = "当前生效版本号")
    private Integer currentVersion;

    @Schema(name = "最新版本号")
    private Integer latestVersion;

    @Schema(name = "备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    @Schema(name = "更新时间")
    private LocalDateTime lastModifiedDate;
}