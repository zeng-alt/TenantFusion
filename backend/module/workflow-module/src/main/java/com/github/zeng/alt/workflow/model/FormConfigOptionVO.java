package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 配置表单下拉选项视图对象（供业务关联表单选择使用）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "配置表单下拉选项")
public class FormConfigOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单ID")
    private Long formConfigId;

    @Schema(name = "表单名称")
    private String name;

    @Schema(name = "表单编码")
    private String code;
}
