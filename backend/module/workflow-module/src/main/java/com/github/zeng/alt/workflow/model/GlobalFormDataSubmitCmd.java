package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 提交流程全局表单数据命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "提交流程全局表单数据请求")
public class GlobalFormDataSubmitCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "流程实例ID不能为空")
    @Schema(name = "运行时流程实例ID", required = true)
    private String processInstanceId;

    @NotBlank(message = "流程模板编码不能为空")
    @Schema(name = "流程模板编码", required = true)
    private String workflowCode;

    @NotBlank(message = "表单数据不能为空")
    @Schema(name = "全局表单字段值（JSON：字段名 → 值）", required = true)
    private String data;
}
