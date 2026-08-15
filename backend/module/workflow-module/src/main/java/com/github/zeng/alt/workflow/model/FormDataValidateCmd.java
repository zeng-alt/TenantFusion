package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 校验动态表单数据命令（只校验，不落库）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "校验动态表单数据请求")
public class FormDataValidateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "表单模板ID不能为空")
    @Schema(name = "表单模板ID", required = true)
    private Long formTemplateId;

    @Schema(name = "表单模板版本快照（缺省取当前生效版本）")
    private Integer formVersion;

    @Schema(name = "表单字段值（JSON：字段名 → 值）")
    private String data;
}
