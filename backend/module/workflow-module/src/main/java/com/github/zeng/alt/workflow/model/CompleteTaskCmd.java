package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 完成任务命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "完成任务请求")
public class CompleteTaskCmd implements Serializable {

    @NotBlank(message = "任务ID不能为空")
    @Schema(name = "任务ID", required = true)
    private String taskId;

    @Schema(name = "审批意见")
    private String comment;

    @Schema(name = "流程变量")
    private Map<String, Object> variables;
}
