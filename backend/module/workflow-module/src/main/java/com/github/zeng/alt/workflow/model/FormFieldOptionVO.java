package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 配置表单字段选项视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "配置表单字段选项")
public class FormFieldOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "选项ID")
    private Long optionId;

    @Schema(name = "选项标签")
    private String label;

    @Schema(name = "选项值")
    private String value;

    @Schema(name = "排序号")
    private Integer sortOrder;
}
