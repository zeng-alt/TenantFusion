package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新流程主数据命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "更新流程请求")
public class WorkflowUpdateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程描述")
    private String description;

    @Schema(name = "流程分类")
    private String category;

    @Schema(name = "备注")
    private String remark;
}
