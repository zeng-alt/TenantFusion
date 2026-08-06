package com.github.zeng.alt.workflow.model;

import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 工作流分页查询基类
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "工作流分页查询")
public class WorkflowPageQuery extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认按创建时间倒序
     */
    public WorkflowPageQuery() {
        setSort("startTime");
        setOrder("desc");
    }
}
