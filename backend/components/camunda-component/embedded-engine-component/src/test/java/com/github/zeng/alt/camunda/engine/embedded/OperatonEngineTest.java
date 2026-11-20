package com.github.zeng.alt.camunda.engine.embedded;

import org.junit.jupiter.api.Test;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.api.CamundaIdentityGroup;
import com.github.zeng.alt.camunda.identity.api.security.CamundaIdentityAuthenticationFilter;
import com.github.zeng.alt.camunda.engine.api.process.StartByDefinitionCmd;
import com.github.zeng.alt.camunda.engine.embedded.process.EmbeddedStartProcessApi;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                "operaton.bpm.authorization.tenant-check-enabled=true",
                "spring.main.banner-mode=off"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OperatonEngineTest {

    private final ProcessEngine engine;
    private final JdbcTemplate jdbc;
    private final ApplicationContext context;

    OperatonEngineTest(ProcessEngine engine, JdbcTemplate jdbc, ApplicationContext context) {
        this.engine = engine;
        this.jdbc = jdbc;
        this.context = context;
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

    @Test
    void selectedTenantLimitsEngineQueries() throws Exception {
        assertThat(context.getBean("camundaIdentityFilterRegistration", FilterRegistrationBean.class).isEnabled())
                .isFalse();
        final var first = engine.getRepositoryService().createDeployment().tenantId("first")
                .addString("first.bpmn", legacyBpmn()).deploy();
        final var second = engine.getRepositoryService().createDeployment().tenantId("second")
                .addString("second.bpmn", legacyBpmn()).deploy();
        try {
            final var starter = new EmbeddedStartProcessApi(engine.getRuntimeService(),
                    engine.getRepositoryService(), engine.getIdentityService());
            assertThat(starter.startByDefinition(StartByDefinitionCmd.builder()
                    .processDefinitionKey("upgrade").tenantId("first").initiator("worker").build())
                    .getInstanceId()).isNotBlank();
            assertThat(engine.getIdentityService().getCurrentAuthentication()).isNull();
            final var user = SecurityUser.withUsername("admin").id("1").tenant("master")
                    .roles(new SimpleGrantedAuthority("home-role")).build();
            user.setActiveTenant("second");
            UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
            TenantContextHolder.setTenantId("second");
            new CamundaIdentityAuthenticationFilter(engine.getIdentityService()).doFilter(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
                        assertThat(engine.getIdentityService().getCurrentAuthentication().getTenantIds())
                                .containsExactly("second");
                        assertThat(engine.getIdentityService().getCurrentAuthentication().getGroupIds()).isEmpty();
                        assertThat(engine.getRepositoryService().createProcessDefinitionQuery().list())
                                .extracting(definition -> definition.getTenantId()).containsExactly("second");
                        assertThat(engine.getRepositoryService().createProcessDefinitionQuery()
                                .deploymentId(first.getId()).count()).isZero();
                        assertThatThrownBy(() -> starter.startByDefinition(StartByDefinitionCmd.builder()
                                .processDefinitionKey("upgrade").tenantId("first").initiator("admin").build()))
                                .isInstanceOf(IllegalStateException.class);
                        assertThat(starter.startByDefinition(StartByDefinitionCmd.builder()
                                .processDefinitionKey("upgrade").tenantId("second").initiator("admin").build())
                                .getInstanceId()).isNotBlank();
                        assertThat(engine.getIdentityService().getCurrentAuthentication().getTenantIds())
                                .containsExactly("second");
                    });
        } finally {
            UserContextHolder.clear();
            TenantContextHolder.clear();
            engine.getRepositoryService().deleteDeployment(first.getId(), true);
            engine.getRepositoryService().deleteDeployment(second.getId(), true);
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
