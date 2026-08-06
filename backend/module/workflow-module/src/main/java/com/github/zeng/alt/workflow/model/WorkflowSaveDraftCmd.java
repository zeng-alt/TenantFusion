package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 保存流程草稿命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "保存流程草稿请求")
public class WorkflowSaveDraftCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "BPMN XML 不能为空")
    @Schema(name = "BPMN XML 内容")
    private String bpmnXml;
}
