package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 按消息名称启动流程命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "按消息名称启动请求")
public class StartByMessageCmd implements Serializable {

    @NotBlank(message = "消息名称不能为空")
    @Schema(name = "消息名称", required = true, example = "Msg_OrderReceived")
    private String messageName;

    @Schema(name = "流程变量")
    private Map<String, Object> variables;

    @Schema(name = "流程实例")
    private String processDefinitionId;

    @Schema(name = "业务key")
    private String businessKey;
}
