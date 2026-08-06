package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 启动流程命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "启动流程请求")
public class StartProcessCmd implements Serializable {

    @NotBlank(message = "流程定义Key不能为空")
    @Schema(name = "流程定义Key", required = true, example = "leave-apply")
    private String processDefinitionKey;

    @Schema(name = "业务键", example = "LEAVE:20240001")
    private String businessKey;

    @Schema(name = "流程变量")
    private Map<String, Object> variables;
}
