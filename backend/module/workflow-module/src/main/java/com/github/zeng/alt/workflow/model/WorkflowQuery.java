package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程分页查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "流程分页查询")
public class WorkflowQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程编码（模糊）")
    private String workflowKey;

    @Schema(name = "流程名称（模糊）")
    private String workflowName;

    @Schema(name = "流程分类")
    private String category;

    public WorkflowQuery() {
        setSort("createdDate");
        setOrder("desc");
    }
}
