package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建业务命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "创建业务请求")
public class BusinessCreateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "业务名称不能为空")
    @Schema(name = "业务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "业务编码不能为空")
    @Schema(name = "业务编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(name = "父业务ID")
    private Long parentId;

    @Schema(name = "排序号")
    private Integer sortOrder;

    @Schema(name = "关联配置表单ID")
    private Long formConfigId;

    @Schema(name = "业务描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}
