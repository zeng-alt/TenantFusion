package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前任务节点定义的表单信息
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "任务节点表单定义")
public class TaskFormDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "任务ID")
    private String taskId;

    @Schema(name = "任务定义Key")
    private String taskDefinitionKey;

    @Schema(name = "任务名称")
    private String taskName;

    @Schema(name = "表单类型：FORM_TEMPLATE-FORM_KIT动态表单，FORM_KEY-前端资源表单，FORM_DATA-内置表单")
    private TaskFormType formType;

    @Schema(name = "表单模板编码（formType=FORM_TEMPLATE，camunda:formRef）")
    private String formRef;

    @Schema(name = "表单模板绑定方式：latest/deployment/version")
    private String formRefBinding;

    @Schema(name = "表单模板版本号（formRefBinding=version）")
    private String formRefVersion;

    @Schema(name = "FormKit 表单定义（formType=FORM_TEMPLATE，取自模板绑定版本）")
    private JsonNode definition;

    @Schema(name = "前端资源表单键（formType=FORM_KEY，camunda:formKey）")
    private String formKey;

    @Schema(name = "内置表单字段（formType=FORM_DATA，camunda:formData）")
    private List<CamundaFormFieldVO> fields;
}
