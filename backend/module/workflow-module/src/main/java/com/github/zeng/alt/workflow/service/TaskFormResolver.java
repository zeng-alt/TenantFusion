package com.github.zeng.alt.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.camunda.engine.api.history.HistoricVariableInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoryApi;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.workflow.entity.BusinessEntity;
import com.github.zeng.alt.workflow.entity.FormConfigVersionEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.CamundaFormFieldOptionVO;
import com.github.zeng.alt.workflow.model.CamundaFormFieldVO;
import com.github.zeng.alt.workflow.model.FormConfigVersionVO;
import com.github.zeng.alt.workflow.model.TaskFormDefinitionVO;
import com.github.zeng.alt.workflow.model.TaskFormType;
import com.github.zeng.alt.workflow.repository.BusinessRepository;
import com.github.zeng.alt.workflow.repository.FormConfigVersionRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaFormData;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前任务节点表单定义解析器。
 * <p>
 * 从 BPMN 用户任务中解析表单定义，支持三种形式：
 * <ul>
 *     <li>FORM_TEMPLATE：{@code camunda:formRef} 引用 FormKit 动态表单模板编码</li>
 *     <li>FORM_KEY：{@code camunda:formKey} 引用前端资源（如 test.vue）</li>
 *     <li>FORM_DATA：{@code camunda:formData} 内联内置表单字段</li>
 * </ul>
 * 同时解析流程绑定的配置表单定义：由流程变量 {@code businessId}/{@code businessCode} 定位业务，
 * 业务关联的 {@code formConfigId} 确定配置表单，版本号取自流程变量 {@code formConfigVersion}
 * （缺失时取当前生效版本）。
 * <p>
 * 表单元数据从 Camunda 部署的流程定义 BPMN XML 解析（经引擎 SPI 的 {@link ProcessDefinitionApi}，
 * camunda-bpmn-model 解析），本地/远程模式通用。
 *
 * @author zengAlt
 */
@Component
@RequiredArgsConstructor
@CommonsLog
public class TaskFormResolver {

    private final TaskApi taskApi;
    private final HistoryApi historyApi;
    private final ProcessDefinitionApi processDefinitionApi;
    private final BusinessRepository businessRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateVersionRepository formTemplateVersionRepository;
    private final FormConfigVersionRepository formConfigVersionRepository;
    private final FormConfigService formConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 解析流程实例当前活动任务节点上定义的表单
     *
     * @param processInstanceId 流程实例ID
     * @return 当前任务表单定义列表（无活动任务或未定义表单时为空）
     */
    public List<TaskFormDefinitionVO> resolveByProcessInstanceId(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return List.of();
        }
        List<TaskInfo> activeTasks = taskApi.getActiveTasks(processInstanceId);
        if (activeTasks.isEmpty()) {
            return List.of();
        }
        BpmnModelInstance model = loadModel(activeTasks.get(0).getProcessDefinitionId());
        if (model == null) {
            return List.of();
        }
        return activeTasks.stream()
                .map(task -> resolve(task, model))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 解析单个任务节点上定义的表单
     *
     * @param taskId 任务ID
     * @return 任务表单定义列表（任务不存在或未定义表单时为空）
     */
    public List<TaskFormDefinitionVO> resolveByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return List.of();
        }
        TaskInfo task = taskApi.get(taskId);
        if (task == null) {
            return List.of();
        }
        BpmnModelInstance model = loadModel(task.getProcessDefinitionId());
        if (model == null) {
            return List.of();
        }
        TaskFormDefinitionVO form = resolve(task, model);
        return form != null ? List.of(form) : List.of();
    }

    /**
     * 从 Camunda 部署的流程定义获取 BPMN 模型（本地/远程模式通用，经引擎 SPI）
     */
    private BpmnModelInstance loadModel(String processDefinitionId) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        try {
            byte[] xml = processDefinitionApi.getBpmnXml(processDefinitionId);
            if (xml == null || xml.length == 0) {
                return null;
            }
            return Bpmn.readModelFromStream(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            log.warn("解析BPMN模型失败: " + processDefinitionId, e);
            return null;
        }
    }

    private TaskFormDefinitionVO resolve(TaskInfo task, BpmnModelInstance model) {
        ModelElementInstance element = model.getModelElementById(task.getTaskDefinitionKey());
        if (!(element instanceof UserTask userTask)) {
            return null;
        }
        TaskFormDefinitionVO.TaskFormDefinitionVOBuilder builder = TaskFormDefinitionVO.builder()
                .taskId(task.getId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .taskName(task.getName());

        String formRef = userTask.getCamundaFormRef();
        if (StringUtils.hasText(formRef)) {
            return builder.formType(TaskFormType.FORM_TEMPLATE)
                    .formRef(formRef)
                    .formRefBinding(userTask.getCamundaFormRefBinding())
                    .formRefVersion(userTask.getCamundaFormRefVersion())
                    .definition(loadTemplateDefinition(formRef, userTask.getCamundaFormRefBinding(), userTask.getCamundaFormRefVersion()))
                    .build();
        }
        String formKey = userTask.getCamundaFormKey();
        if (StringUtils.hasText(formKey)) {
            return builder.formType(TaskFormType.FORM_KEY).formKey(formKey).build();
        }
        List<CamundaFormFieldVO> fields = loadFormDataFields(userTask);
        if (!fields.isEmpty()) {
            return builder.formType(TaskFormType.FORM_DATA).fields(fields).build();
        }
        return null;
    }

    /**
     * 解析流程绑定的配置表单定义：
     * 由流程变量定位业务，取业务关联的配置表单；版本号取自流程变量 formConfigVersion，缺失时取当前生效版本
     */
    public FormConfigVersionVO resolveConfigForm(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        Map<String, Object> variables = loadProcessVariables(processInstanceId);
        BusinessEntity business = resolveBusiness(variables);
        if (business == null || business.getFormConfigId() == null) {
            return null;
        }
        Long formConfigId = business.getFormConfigId();
        Integer version = asInteger(variables.get("formConfigVersion"));
        if (version != null) {
            return formConfigService.getVersion(formConfigId, version);
        }
        return formConfigVersionRepository.findFirstByFormConfigIdAndCurrentTrue(formConfigId)
                .map(versionEntity -> formConfigService.getVersion(versionEntity.getVersionId()))
                .orElse(null);
    }

    private BusinessEntity resolveBusiness(Map<String, Object> variables) {
        Long businessId = asLong(variables.get("businessId"));
        if (businessId != null) {
            BusinessEntity byId = businessRepository.findById(businessId).getOrElse((BusinessEntity) null);
            if (byId != null) {
                return byId;
            }
        }
        String businessCode = asString(variables.get("businessCode"));
        if (StringUtils.hasText(businessCode)) {
            BusinessEntity byCode = businessRepository.findByCode(businessCode).orElse(null);
            if (byCode != null) {
                return byCode;
            }
        }
        return null;
    }

    private Map<String, Object> loadProcessVariables(String processInstanceId) {
        Map<String, Object> map = new HashMap<>();
        for (HistoricVariableInfo variable : historyApi.variables(processInstanceId)) {
            map.put(variable.getName(), variable.getValue());
        }
        return map;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 按 formRefBinding 解析表单模板定义：
     * version 绑定取指定版本号，latest/deployment 取当前生效版本
     */
    private JsonNode loadTemplateDefinition(String code, String binding, String version) {
        FormTemplateEntity template = formTemplateRepository.findByCode(code).orElse(null);
        if (template == null) {
            return null;
        }
        Optional<FormTemplateVersionEntity> versionOpt;
        if ("version".equalsIgnoreCase(binding) && StringUtils.hasText(version)) {
            versionOpt = formTemplateVersionRepository.findFirstByFormTemplateIdAndVersion(template.getFormTemplateId(), Integer.valueOf(version));
        } else {
            versionOpt = formTemplateVersionRepository.findFirstByFormTemplateIdAndCurrentTrue(template.getFormTemplateId());
        }
        return versionOpt.map(FormTemplateVersionEntity::getDefinition).map(this::parseDefinition).orElse(null);
    }

    private JsonNode parseDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(definition);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<CamundaFormFieldVO> loadFormDataFields(UserTask userTask) {
        ExtensionElements extensionElements = userTask.getExtensionElements();
        if (extensionElements == null) {
            return List.of();
        }
        CamundaFormData formData = extensionElements.getElementsQuery()
                .filterByType(CamundaFormData.class)
                .singleResult();
        if (formData == null) {
            return List.of();
        }
        return formData.getCamundaFormFields().stream().map(field -> CamundaFormFieldVO.builder()
                .id(field.getCamundaId())
                .label(field.getCamundaLabel())
                .type(field.getCamundaType())
                .defaultValue(field.getCamundaDefaultValue())
                .datePattern(field.getCamundaDatePattern())
                .values(field.getCamundaValues().stream()
                        .map(value -> CamundaFormFieldOptionVO.builder()
                                .id(value.getCamundaId())
                                .name(value.getCamundaName())
                                .build())
                        .toList())
                .build()).toList();
    }
}
