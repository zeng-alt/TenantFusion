package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 配置表单分页查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "配置表单分页查询")
public class FormConfigQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单名称（模糊）")
    private String name;

    @Schema(name = "表单编码（模糊）")
    private String code;

    @Schema(name = "表单分类")
    private String category;

    public FormConfigQuery() {
        setSort("createdDate");
        setOrder("desc");
    }
}
