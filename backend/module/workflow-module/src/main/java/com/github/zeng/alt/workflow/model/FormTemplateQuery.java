package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 动态表单模板分页查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "动态表单模板分页查询")
public class FormTemplateQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "模板名称（模糊）")
    private String name;

    @Schema(name = "模板编码（模糊）")
    private String code;

    @Schema(name = "模板分类")
    private String category;

    public FormTemplateQuery() {
        setSort("createdDate");
        setOrder("desc");
    }
}