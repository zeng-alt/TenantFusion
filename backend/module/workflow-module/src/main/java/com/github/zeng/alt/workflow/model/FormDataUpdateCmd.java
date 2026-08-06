package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新动态表单数据命令
 * <p>
 * 支持部分更新：仅请求体中非 null 的字段会被覆盖（null 表示不修改）。
 *
 * @author zengAlt
 */
@Data
@Schema(name = "更新动态表单数据请求")
public class FormDataUpdateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "数据ID")
    private Long formDataId;

    @Schema(name = "表单模板版本快照")
    private Integer formVersion;

    @Schema(name = "关联流程实例ID")
    private String processInstanceId;

    @Schema(name = "表单字段值（JSON：字段名 → 值）")
    private String data;

    @Schema(name = "数据状态")
    private FormDataStatus status;

    @Schema(name = "备注")
    private String remark;
}