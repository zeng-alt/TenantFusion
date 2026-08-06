package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 签收任务命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "签收任务请求")
public class ClaimTaskCmd implements Serializable {

    @NotBlank(message = "任务ID不能为空")
    @Schema(name = "任务ID", required = true)
    private String taskId;

    @Schema(name = "签收人ID（默认当前用户）")
    private String userId;
}
