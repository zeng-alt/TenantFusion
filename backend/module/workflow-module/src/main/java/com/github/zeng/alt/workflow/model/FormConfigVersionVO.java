package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置表单版本视图对象（含嵌套字段树）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "配置表单版本")
public class FormConfigVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "版本ID")
    private Long versionId;

    @Schema(name = "配置表单ID")
    private Long formConfigId;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "版本状态")
    private FormConfigVersionStatus status;

    @Schema(name = "是否当前生效版本")
    private Boolean current;

    @Schema(name = "发布时间")
    private LocalDateTime publishedDate;

    @Schema(name = "发布人")
    private String publishedBy;

    @Schema(name = "版本备注")
    private String remark;

    @Schema(name = "标签宽度（px）")
    private Integer labelWidth;

    @Schema(name = "标签位置：left / top")
    private String labelPlacement;

    @Schema(name = "标签对齐方式：left / right")
    private String labelAlign;

    @Schema(name = "表单尺寸：small / medium / large")
    private String formSize;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    /** 顶层字段列表（不含嵌套子字段，嵌套子字段在 FormFieldVO.children 中） */
    @Schema(name = "字段列表")
    private List<FormFieldVO> fields;
}
