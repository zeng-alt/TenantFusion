package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程全局表单定义
 * <p>
 * 由流程级 BPMN XML 的 camunda:property（globalForm.*）解析得出。
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "流程全局表单定义")
public class GlobalFormDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "全局表单类型：CAMUNDA-Camunda表单模板，EXTERNAL-外部前端资源表单，GENERATED-内联生成表单")
    private GlobalFormType type;

    @Schema(name = "表单模板编码（type=CAMUNDA，globalForm.formRef）")
    private String formRef;

    @Schema(name = "表单模板绑定方式（type=CAMUNDA）：deployment-最新已上线版本，latest-最新未下线版本（含草稿），version-指定版本号")
    private String formRefBinding;

    @Schema(name = "表单模板版本号（formRefBinding=version）")
    private String formRefVersion;

    @Schema(name = "表单定义（type=CAMUNDA，取自模板绑定版本的 FormKit FormDefinition JSON）")
    private JsonNode definition;

    @Schema(name = "外部前端资源表单地址（type=EXTERNAL，globalForm.formKey）")
    private String formKey;

    @Schema(name = "内联表单定义（type=GENERATED，globalForm.fields）")
    private JsonNode fields;
}
