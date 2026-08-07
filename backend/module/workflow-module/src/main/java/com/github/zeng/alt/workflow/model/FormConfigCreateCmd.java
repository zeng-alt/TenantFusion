package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建配置表单命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "创建配置表单请求")
public class FormConfigCreateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "表单名称不能为空")
    @Schema(name = "表单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "表单编码不能为空")
    @Schema(name = "表单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(name = "表单分类")
    private String category;

    @Schema(name = "表单描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}
