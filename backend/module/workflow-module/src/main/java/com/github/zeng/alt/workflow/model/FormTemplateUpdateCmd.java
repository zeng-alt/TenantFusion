package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新动态表单模板命令
 * <p>
 * 支持部分更新：仅请求体中非 null 的字段会被覆盖（null 表示不修改）。
 *
 * @author zengAlt
 */
@Data
@Schema(name = "更新动态表单模板请求")
public class FormTemplateUpdateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "模板分类")
    private String category;

    @Schema(name = "模板描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}