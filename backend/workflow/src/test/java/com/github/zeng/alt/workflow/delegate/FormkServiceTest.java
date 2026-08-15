package com.github.zeng.alt.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.zeng.alt.json.JacksonHelper;
import com.github.zeng.alt.workflow.model.GlobalFormDataSubmitCmd;
import com.github.zeng.alt.workflow.model.GlobalFormDataVO;
import com.github.zeng.alt.workflow.service.GlobalFormDataService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormkServiceTest {

    private GlobalFormDataService globalFormDataService;
    private FormkService formkService;
    private DelegateExecution execution;
    private ArgumentCaptor<GlobalFormDataSubmitCmd> cmdCaptor;
    private ObjectMapper objectMapper;
    private Map<String, Object> localVariables;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        ObjectProvider<ObjectMapper> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(objectMapper);

        globalFormDataService = mock(GlobalFormDataService.class);
        formkService = new FormkService(globalFormDataService, new JacksonHelper(provider));
        execution = mock(DelegateExecution.class);
        cmdCaptor = ArgumentCaptor.forClass(GlobalFormDataSubmitCmd.class);
        localVariables = new HashMap<>();

        when(execution.getProcessInstanceId()).thenReturn("pi-1");
        when(execution.getProcessDefinitionId()).thenReturn("testProcess:1:uuid");
        when(execution.getVariableNamesLocal()).thenReturn(localVariables.keySet());
        when(execution.getVariableLocal(any())).thenAnswer(invocation -> localVariables.get(invocation.getArgument(0)));
    }

    @Test
    void appliesAddAndDeleteOnExistingData() throws Exception {
        GlobalFormDataVO existing = GlobalFormDataVO.builder()
                .workflowCode("testProcess")
                .data(objectMapper.readTree("{\"test\":\"old\",\"oldField\":\"x\",\"keep\":1}"))
                .build();
        when(globalFormDataService.getByProcessInstanceId("pi-1")).thenReturn(existing);
        localVariables.put("formk:add:test", "test");
        localVariables.put("formk:add:fromVar", 42);
        localVariables.put("formk:delete:oldField", "");

        formkService.execute(execution);

        verify(globalFormDataService).submit(cmdCaptor.capture());
        GlobalFormDataSubmitCmd cmd = cmdCaptor.getValue();
        assertThat(cmd.getProcessInstanceId()).isEqualTo("pi-1");
        assertThat(cmd.getWorkflowCode()).isEqualTo("testProcess");
        ObjectNode data = (ObjectNode) objectMapper.readTree(cmd.getData());
        assertThat(data.get("test").asText()).isEqualTo("test");
        assertThat(data.get("fromVar").asInt()).isEqualTo(42);
        assertThat(data.get("keep").asInt()).isEqualTo(1);
        assertThat(data.has("oldField")).isFalse();
    }

    @Test
    void initializesDataWhenNoExistingRecord() throws Exception {
        when(globalFormDataService.getByProcessInstanceId("pi-1")).thenReturn(null);
        localVariables.put("formk:add:test", "test");
        localVariables.put("formk:add:fromVar", 42);

        formkService.execute(execution);

        verify(globalFormDataService).submit(cmdCaptor.capture());
        GlobalFormDataSubmitCmd cmd = cmdCaptor.getValue();
        assertThat(cmd.getWorkflowCode()).isEqualTo("testProcess");
        ObjectNode data = (ObjectNode) objectMapper.readTree(cmd.getData());
        assertThat(data.get("test").asText()).isEqualTo("test");
        assertThat(data.get("fromVar").asInt()).isEqualTo(42);
    }

    @Test
    void ignoresUnrelatedLocalVariables() {
        localVariables.put("someOtherVar", "x");
        when(globalFormDataService.getByProcessInstanceId("pi-1")).thenReturn(null);

        formkService.execute(execution);

        verify(globalFormDataService, never()).submit(Mockito.any());
    }
}
