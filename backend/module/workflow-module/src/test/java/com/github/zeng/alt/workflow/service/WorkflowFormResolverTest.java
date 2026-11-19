package com.github.zeng.alt.workflow.service;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.workflow.model.GlobalFormType;
import com.github.zeng.alt.workflow.model.TaskFormType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证模型 API 迁移后仍能解析旧扩展及 Operaton 扩展中的表单。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WorkflowFormResolverTest {

    @ParameterizedTest
    @ValueSource(strings = {"camunda", "operaton"})
    void resolvesGlobalFormPropertiesFromEitherNamespace(String vendor) {
        final var resolver = new GlobalFormResolver(mock(), mock(), JsonMapper.builder().build());
        final var form = resolver.resolve(bpmn(vendor));

        assertThat(form).isNotNull();
        assertThat(form.getType()).isEqualTo(GlobalFormType.EXTERNAL);
        assertThat(form.getFormKey()).isEqualTo("/forms/global.vue");
    }

    @ParameterizedTest
    @ValueSource(strings = {"camunda", "operaton"})
    void resolvesGeneratedTaskFieldsAndOptionsFromEitherNamespace(String vendor) {
        final TaskApi taskApi = mock();
        final ProcessDefinitionApi definitions = mock();
        when(taskApi.get("task-1")).thenReturn(TaskInfo.builder()
                .id("task-1").processDefinitionId("definition-1").taskDefinitionKey("approve").build());
        when(definitions.getBpmnXml("definition-1")).thenReturn(bpmn(vendor).getBytes(StandardCharsets.UTF_8));
        final var resolver = new TaskFormResolver(
                taskApi,
                mock(),
                definitions,
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                JsonMapper.builder().build()
        );

        final var forms = resolver.resolveByTaskId("task-1");

        assertThat(forms).hasSize(1);
        assertThat(forms.getFirst().getFormType()).isEqualTo(TaskFormType.FORM_DATA);
        final var field = forms.getFirst().getFields().getFirst();
        assertThat(field.getId()).isEqualTo("decision");
        assertThat(field.getDefaultValue()).isEqualTo("yes");
        assertThat(field.getValues().getFirst().getName()).isEqualTo("Approve");
    }

    private static String bpmn(String vendor) {
        return """
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:ext="http://%s.org/schema/1.0/bpmn" targetNamespace="upgrade">
                  <process id="upgrade" isExecutable="true">
                    <extensionElements><ext:properties>
                      <ext:property name="globalForm.type" value="external"/>
                      <ext:property name="globalForm.formKey" value="/forms/global.vue"/>
                    </ext:properties></extensionElements>
                    <userTask id="approve"><extensionElements><ext:formData>
                      <ext:formField id="decision" label="Decision" type="enum" defaultValue="yes">
                        <ext:value id="yes" name="Approve"/>
                      </ext:formField>
                    </ext:formData></extensionElements></userTask>
                  </process>
                </definitions>
                """.formatted(vendor);
    }
}
