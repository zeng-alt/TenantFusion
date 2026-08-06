package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建动态表单模板命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "创建动态表单模板请求")
public class FormTemplateCreateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "模板名称不能为空")
    @Schema(name = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "请假申请")
    private String name;

    @NotBlank(message = "模板编码不能为空")
    @Schema(name = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "leave_request")
    private String code;

    @Schema(name = "模板分类", example = "人事")
    private String category;

    @Schema(name = "模板描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}