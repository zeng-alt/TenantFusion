package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动态表单模板视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "动态表单模板")
public class FormTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "模板ID")
    private Long formTemplateId;

    @Schema(name = "模板名称")
    private String name;

    @Schema(name = "模板编码")
    private String code;

    @Schema(name = "模板分类")
    private String category;

    @Schema(name = "当前生效版本号")
    private Integer currentVersion;

    @Schema(name = "最新版本号（含草稿）")
    private Integer latestVersion;

    @Schema(name = "模板描述")
    private String description;

    @Schema(name = "备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    @Schema(name = "更新时间")
    private LocalDateTime lastModifiedDate;
}