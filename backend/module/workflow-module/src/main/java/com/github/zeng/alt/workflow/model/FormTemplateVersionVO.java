package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表单模板版本视图对象
 *
 * @author zengAlt
 */
@Data
@Schema(name = "表单模板版本")
public class FormTemplateVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "版本ID")
    private Long versionId;

    @Schema(name = "表单模板ID")
    private Long formTemplateId;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "版本状态")
    private FormTemplateVersionStatus status;

    @Schema(name = "是否当前生效版本")
    private Boolean current;

    @Schema(name = "表单定义（FormKit FormDefinition JSON）")
    private JsonNode definition;

    @Schema(name = "发布时间")
    private LocalDateTime publishedDate;

    @Schema(name = "发布人")
    private String publishedBy;

    @Schema(name = "版本备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;
}