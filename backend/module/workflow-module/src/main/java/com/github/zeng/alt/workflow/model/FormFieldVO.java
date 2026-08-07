package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 配置表单字段视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "配置表单字段")
public class FormFieldVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "字段ID")
    private Long fieldId;

    @Schema(name = "父字段ID")
    private Long parentFieldId;

    @Schema(name = "字段标识")
    private String fieldKey;

    @Schema(name = "字段标签")
    private String fieldLabel;

    @Schema(name = "字段类型")
    private FieldType fieldType;

    @Schema(name = "默认值")
    private String defaultValue;

    @Schema(name = "占位提示")
    private String placeholder;

    @Schema(name = "帮助文本")
    private String helpText;

    @Schema(name = "排序号")
    private Integer sortOrder;

    @Schema(name = "栅格列宽")
    private Integer colSpan;

    @Schema(name = "是否必填")
    private Boolean required;

    @Schema(name = "是否只读")
    private Boolean readonly;

    @Schema(name = "是否隐藏")
    private Boolean hidden;

    @Schema(name = "校验规则JSON")
    private String validationRules;

    @Schema(name = "条件渲染JSON")
    private String visibilityCondition;

    @Schema(name = "字段属性JSON")
    private String fieldProps;

    @Schema(name = "选项列表（仅 SELECT / MULTI_SELECT 类型）")
    private List<FormFieldOptionVO> options;

    @Schema(name = "子字段列表（仅 LIST / MAP / OBJECT 类型）")
    private List<FormFieldVO> children;
}
