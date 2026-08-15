package com.github.zeng.alt.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;
import com.github.zeng.alt.workflow.model.GlobalFormType;
import com.github.zeng.alt.workflow.repository.FormTemplateRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 流程全局表单解析器。
 * <p>
 * 从流程级 BPMN XML 的 camunda:property（globalForm.*）解析全局表单定义，支持三种类型：
 * <ul>
 *     <li>CAMUNDA：globalForm.formRef 引用 FormKit 动态表单模板编码，绑定方式（globalForm.binding）：
 *         deployment-最新已上线版本，latest-最新未下线版本（含草稿），version-按版本号</li>
 *     <li>EXTERNAL：globalForm.formKey 引用前端 vue 地址</li>
 *     <li>GENERATED：globalForm.fields 内联表单定义</li>
 * </ul>
 * 表单元数据从流程定义部署的 BPMN XML 解析（camunda-bpmn-model），不依赖运行中的引擎。
 *
 * @author zengAlt
 */
@Component
@RequiredArgsConstructor
@CommonsLog
public class GlobalFormResolver {

    private static final String PREFIX = "globalForm.";

    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateVersionRepository formTemplateVersionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 从 BPMN XML 解析流程全局表单定义
     *
     * @param bpmnXml 流程定义 XML
     * @return 全局表单定义（未配置或解析失败时返回 null）
     */
    public GlobalFormDefinitionVO resolve(String bpmnXml) {
        if (!StringUtils.hasText(bpmnXml)) {
            return null;
        }
        try {
            BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(
                    bpmnXml.getBytes(StandardCharsets.UTF_8)));
            return resolve(model);
        } catch (Exception e) {
            log.warn("解析全局表单失败", e);
            return null;
        }
    }

    private GlobalFormDefinitionVO resolve(BpmnModelInstance model) {
        Process process = model.getModelElementsByType(Process.class).stream().findFirst().orElse(null);
        if (process == null) {
            return null;
        }
        Map<String, String> props = readProperties(process);
        String type = props.get(PREFIX + "type");
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return switch (type) {
            case "camunda" -> resolveCamunda(props);
            case "external" -> resolveExternal(props);
            case "generated" -> resolveGenerated(props);
            default -> null;
        };
    }

    /**
     * 读取流程级 extensionElements 下的 camunda:properties
     */
    private Map<String, String> readProperties(Process process) {
        Map<String, String> map = new HashMap<>();
        ExtensionElements extensionElements = process.getExtensionElements();
        if (extensionElements == null) {
            return map;
        }
        CamundaProperties properties = extensionElements.getElementsQuery()
                .filterByType(CamundaProperties.class)
                .singleResult();
        if (properties == null) {
            return map;
        }
        for (CamundaProperty property : properties.getCamundaProperties()) {
            map.put(property.getCamundaName(), property.getCamundaValue());
        }
        return map;
    }

    /**
     * CAMUNDA：按绑定方式解析 FormKit 动态表单模板
     */
    private GlobalFormDefinitionVO resolveCamunda(Map<String, String> props) {
        String formRef = props.get(PREFIX + "formRef");
        if (!StringUtils.hasText(formRef)) {
            return null;
        }
        String binding = props.getOrDefault(PREFIX + "binding", "deployment");
        FormTemplateEntity template = formTemplateRepository.findByCode(formRef).orElse(null);
        if (template == null) {
            return null;
        }
        Optional<FormTemplateVersionEntity> version = resolveVersion(template, binding, props.get(PREFIX + "version"));
        return version.map(v -> GlobalFormDefinitionVO.builder()
                .type(GlobalFormType.CAMUNDA)
                .formRef(formRef)
                .formRefBinding(binding)
                .formRefVersion(String.valueOf(v.getVersion()))
                .definition(parseJson(v.getDefinition()))
                .build()).orElse(null);
    }

    /**
     * 按绑定方式解析模板版本：
     * deployment-当前生效（最新已上线）版本，latest-最新未下线版本（含草稿），version-指定版本号
     */
    private Optional<FormTemplateVersionEntity> resolveVersion(FormTemplateEntity template, String binding, String version) {
        if ("version".equalsIgnoreCase(binding)) {
            Integer versionNo = parseInteger(version);
            if (versionNo == null) {
                return Optional.empty();
            }
            return formTemplateVersionRepository
                    .findFirstByFormTemplateIdAndVersion(template.getFormTemplateId(), versionNo);
        }
        if ("latest".equalsIgnoreCase(binding)) {
            return formTemplateVersionRepository
                    .findByFormTemplateIdOrderByVersionDesc(template.getFormTemplateId())
                    .stream()
                    .filter(v -> v.getStatus() != FormTemplateVersionStatus.OFFLINE)
                    .findFirst();
        }
        return formTemplateVersionRepository.findFirstByFormTemplateIdAndCurrentTrue(template.getFormTemplateId());
    }

    /**
     * EXTERNAL：返回前端 vue 地址
     */
    private GlobalFormDefinitionVO resolveExternal(Map<String, String> props) {
        String formKey = props.get(PREFIX + "formKey");
        if (!StringUtils.hasText(formKey)) {
            return null;
        }
        return GlobalFormDefinitionVO.builder()
                .type(GlobalFormType.EXTERNAL)
                .formKey(formKey)
                .build();
    }

    /**
     * GENERATED：返回内联表单定义
     */
    private GlobalFormDefinitionVO resolveGenerated(Map<String, String> props) {
        String fields = props.get(PREFIX + "fields");
        if (!StringUtils.hasText(fields)) {
            return null;
        }
        return GlobalFormDefinitionVO.builder()
                .type(GlobalFormType.GENERATED)
                .fields(parseJson(fields))
                .build();
    }

    private JsonNode parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("解析 JSON 失败", e);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
