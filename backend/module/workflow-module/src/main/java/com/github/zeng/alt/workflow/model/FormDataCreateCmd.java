package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建动态表单数据命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "创建动态表单数据请求")
public class FormDataCreateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "表单模板ID不能为空")
    @Schema(name = "表单模板ID", required = true)
    private Long formTemplateId;

    @Schema(name = "表单模板版本快照", example = "1")
    private Integer formVersion = 1;

    @Schema(name = "关联流程实例ID")
    private String processInstanceId;

    @Schema(name = "表单字段值（JSON：字段名 → 值）")
    private String data;

    @Schema(name = "数据状态")
    private FormDataStatus status = FormDataStatus.SUBMITTED;

    @Schema(name = "备注")
    private String remark;
}