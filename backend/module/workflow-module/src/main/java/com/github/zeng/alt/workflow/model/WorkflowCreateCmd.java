package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建流程命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "创建流程请求")
public class WorkflowCreateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "流程编码不能为空")
    @Schema(name = "流程编码", required = true, example = "leave-approval")
    private String workflowKey;

    @NotBlank(message = "流程名称不能为空")
    @Schema(name = "流程名称", required = true, example = "请假审批流程")
    private String workflowName;

    @Schema(name = "流程描述")
    private String description;

    @Schema(name = "流程分类", example = "人事")
    private String category;

    @Schema(name = "备注")
    private String remark;
}
