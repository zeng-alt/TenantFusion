package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 历史流程实例查询参数
 *
 * @author zengAlt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "历史流程实例查询参数")
public class HistoricProcessInstanceQuery extends WorkflowPageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "流程定义名称（模糊匹配）")
    private String processDefinitionName;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "流程状态：running-进行中，completed-已完成，terminated-已终止，suspended-已挂起")
    private String state;

    @Schema(name = "启动用户ID")
    private String startUserId;
}
