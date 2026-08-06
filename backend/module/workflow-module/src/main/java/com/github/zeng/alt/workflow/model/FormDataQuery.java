package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 动态表单数据分页查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "动态表单数据分页查询")
public class FormDataQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单模板ID")
    private Long formTemplateId;

    @Schema(name = "关联流程实例ID")
    private String processInstanceId;

    @Schema(name = "数据状态")
    private FormDataStatus status;

    public FormDataQuery() {
        setSort("createdDate");
        setOrder("desc");
    }
}