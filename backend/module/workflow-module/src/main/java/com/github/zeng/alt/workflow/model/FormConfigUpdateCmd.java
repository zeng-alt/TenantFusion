package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新配置表单命令（部分更新，null 字段不修改）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "更新配置表单请求")
public class FormConfigUpdateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单名称")
    private String name;

    @Schema(name = "表单分类")
    private String category;

    @Schema(name = "表单描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}
