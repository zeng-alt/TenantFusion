package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 节点执行状态
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "节点执行状态")
public class NodeExecutionState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "执行状态：pending/active/completed/rejected")
    private ExecutionStatus status;

    @Schema(name = "访问次数")
    private Integer visitCount;

    @Schema(name = "驳回次数")
    private Integer rejectCount;

    @Schema(name = "办理人")
    private String assignee;

    @Schema(name = "候选用户")
    private List<String> candidateUsers;

    @Schema(name = "候选用户组")
    private List<String> candidateGroups;
}
