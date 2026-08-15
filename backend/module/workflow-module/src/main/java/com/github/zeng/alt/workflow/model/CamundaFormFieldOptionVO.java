package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 内置表单字段选项（camunda:value）
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "内置表单字段选项")
public class CamundaFormFieldOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "选项值")
    private String id;

    @Schema(name = "选项名称")
    private String name;
}
