package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 连线执行状态
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "连线执行状态")
public class FlowExecutionState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "执行状态：pending/active/completed/rejected")
    private ExecutionStatus status;

    @Schema(name = "访问次数")
    private Integer visitCount;
}
