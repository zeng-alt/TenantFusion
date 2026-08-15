package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 内置表单字段（camunda:formField）
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "内置表单字段")
public class CamundaFormFieldVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "字段ID")
    private String id;

    @Schema(name = "字段标签")
    private String label;

    @Schema(name = "字段类型（string/long/date/boolean/enum 等）")
    private String type;

    @Schema(name = "默认值")
    private String defaultValue;

    @Schema(name = "日期格式（type=date）")
    private String datePattern;

    @Schema(name = "选项列表（type=enum）")
    private List<CamundaFormFieldOptionVO> values;
}
