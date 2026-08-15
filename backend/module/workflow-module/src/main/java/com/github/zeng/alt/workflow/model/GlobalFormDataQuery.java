package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程全局表单数据分页查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "流程全局表单数据分页查询")
public class GlobalFormDataQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程模板编码")
    private String workflowCode;

    @Schema(name = "运行时流程实例ID")
    private String processInstanceId;

    public GlobalFormDataQuery() {
        setSort("lastModifiedDate");
        setOrder("desc");
    }
}
