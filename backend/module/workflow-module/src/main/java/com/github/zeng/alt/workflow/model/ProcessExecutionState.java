package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流程执行状态（供 BpmnProcessViewer 高亮/时间线使用）
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "流程执行状态")
public class ProcessExecutionState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "流程实例ID")
    private String processInstanceId;

    @Schema(name = "节点执行状态（key 为 BPMN 元素ID）")
    private Map<String, NodeExecutionState> elements;

    @Schema(name = "执行顺序（按节点访问先后）")
    private List<String> executionOrder;

    @Schema(name = "节点访问时间（与 executionOrder 对应）")
    private List<String> timestamps;

    @Schema(name = "节点结果（与 executionOrder 对应）")
    private List<String> results;
}
