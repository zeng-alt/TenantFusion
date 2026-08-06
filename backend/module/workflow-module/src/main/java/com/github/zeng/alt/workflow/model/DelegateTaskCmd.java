package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 委派任务命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "委派任务请求")
public class DelegateTaskCmd implements Serializable {

    @NotBlank(message = "任务ID不能为空")
    @Schema(name = "任务ID", required = true)
    private String taskId;

    @NotBlank(message = "委派目标用户ID不能为空")
    @Schema(name = "委派目标用户ID", required = true)
    private String userId;
}
