package com.github.zeng.alt.camunda.engine.embedded;

import org.junit.jupiter.api.Test;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.api.CamundaIdentityGroup;
import org.springframework.context.annotation.Bean;
import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.variable.Variables;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Boot 4 自动装配真实 Operaton 引擎并运行既有 Camunda BPMN 扩展。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@SpringBootTest(classes = OperatonEngineTest.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:operaton-upgrade;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "operaton.bpm.database.schema-update=true",
                "operaton.bpm.job-execution.enabled=false",
                "operaton.bpm.history-level=full",
                "spring.main.banner-mode=off"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OperatonEngineTest {

    private final ProcessEngine engine;
    private final JdbcTemplate jdbc;

    OperatonEngineTest(ProcessEngine engine, JdbcTemplate jdbc) {
        this.engine = engine;
        this.jdbc = jdbc;
    }

    @Test
    void deploysLegacyBpmnStartsTenantProcessAndCompletesTaskWithJsonVariable() {
        final var deployment = engine.getRepositoryService().createDeployment().tenantId("upgrade-tenant")
                .addString("upgrade.bpmn", legacyBpmn()).deploy();
        try {
            final var variables = Variables.createVariables().putValueTyped("payload", Variables.objectValue(
                    Map.of("amount", 42)).serializationDataFormat("application/json").create());
            final var instance = engine.getRuntimeService().createProcessInstanceByKey("upgrade")
                    .processDefinitionTenantId("upgrade-tenant").businessKey("upgrade-1")
                    .setVariables(variables).execute();
            final var task = engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).singleResult();

            assertThat(engine.getIdentityService().createGroupQuery().groupMember("demo").list())
                    .extracting(org.operaton.bpm.engine.identity.Group::getId).containsExactly("approvers");
            assertThat(task.getAssignee()).isEqualTo("demo");
            assertThat(task.getTenantId()).isEqualTo("upgrade-tenant");
            assertThat(engine.getRuntimeService().getVariable(instance.getId(), "payload"))
                    .isEqualTo(Map.of("amount", 42));
            engine.getTaskService().complete(task.getId());
            assertThat(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance.getId()).count())
                    .isZero();
            assertThat(engine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(instance.getId()).finished().count()).isEqualTo(1);
            assertThat(jdbc.queryForList("select VERSION_ from ACT_GE_SCHEMA_LOG", String.class)).contains("7.25.0");
        } finally {
            engine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    private static String legacyBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:camunda="http://camunda.org/schema/1.0/bpmn" targetNamespace="upgrade">
                  <process id="upgrade" isExecutable="true" camunda:historyTimeToLive="1">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                    <userTask id="approve" name="Approve" camunda:assignee="demo"/>
                    <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        CamundaUserGroupSource userGroupSource() {
            final CamundaUserGroupSource source = mock();
            when(source.findGroupsByUsername("demo"))
                    .thenReturn(List.of(new CamundaIdentityGroup("approvers", "Approvers", "group")));
            return source;
        }
    }
}
