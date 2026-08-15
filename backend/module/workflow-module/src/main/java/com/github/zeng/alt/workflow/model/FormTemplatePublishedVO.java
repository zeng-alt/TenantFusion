package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 运行态表单定义视图对象（按模板编码取当前已发布定义，供填表组件使用）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "运行态表单定义")
public class FormTemplatePublishedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单模板ID")
    private Long formTemplateId;

    @Schema(name = "模板名称")
    private String name;

    @Schema(name = "模板编码")
    private String code;

    @Schema(name = "当前生效版本号（0 表示从未发布）")
    private Integer currentVersion;

    @Schema(name = "定义来源版本号")
    private Integer version;

    @Schema(name = "表单定义（FormKit FormDefinition JSON）")
    private JsonNode definition;
}
