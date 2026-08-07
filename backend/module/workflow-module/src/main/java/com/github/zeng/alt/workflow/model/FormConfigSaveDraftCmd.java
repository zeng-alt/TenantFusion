package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 保存配置表单草稿命令（包含完整字段树）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "保存配置表单草稿请求")
public class FormConfigSaveDraftCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "字段列表不能为空")
    @Schema(name = "顶层字段列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FormFieldVO> fields;

    @Schema(name = "标签宽度（px）")
    private Integer labelWidth;

    @Schema(name = "标签位置：left / top")
    private String labelPlacement;

    @Schema(name = "标签对齐方式：left / right")
    private String labelAlign;

    @Schema(name = "表单尺寸：small / medium / large")
    private String formSize;
}
