package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 按流程定义Key在指定节点启动流程命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "按流程定义Key在指定节点启动请求")
public class StartByDefinitionAtElementCmd implements Serializable {

    @NotBlank(message = "流程定义Key不能为空")
    @Schema(name = "流程定义Key", required = true, example = "MyExampleProcessKey")
    private String processDefinitionKey;

    @NotBlank(message = "启动节点元素ID不能为空")
    @Schema(name = "启动节点元素ID", required = true, example = "Activity_ProcessOrder")
    private String elementId;

    @Schema(name = "流程变量")
    private Map<String, Object> variables;

    @Schema(name = "流程实例")
    private String processDefinitionId;

    @Schema(name = "业务key")
    private String businessKey;
}
