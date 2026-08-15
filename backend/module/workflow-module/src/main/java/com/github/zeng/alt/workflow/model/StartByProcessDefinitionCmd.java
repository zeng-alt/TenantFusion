package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 按流程定义Key启动流程命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "按流程定义Key启动请求")
public class StartByProcessDefinitionCmd implements Serializable {

    @NotBlank(message = "流程定义Key不能为空")
    @Schema(name = "流程定义Key", required = true, example = "MyExampleProcessKey")
    private String processDefinitionKey;

    @Schema(name = "流程实例")
    private String processDefinitionId;

    @Schema(name = "流程变量")
    private Map<String, Object> variables;

    @Schema(name = "业务key")
    private String businessKey;
}
