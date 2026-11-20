package com.github.zeng.alt.security.abac.server.web;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 服务端 HTTP 协议与实际 HTTP/ProblemDetail 状态一致性回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyControllerTest {
    @Test
    void returnsSnapshotDirectlyWithoutWrappingExpressionObjects() throws Exception {
        final MockMvc mvc = standaloneSetup(new AbacPolicyController(query -> Either.right(
                new AbacPolicySnapshotDto(query.tenant(), query.permission(), "empty", List.of())))).build();
        mvc.perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "update"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tenant").value("t001"))
                .andExpect(jsonPath("$.revision").value("empty")).andExpect(jsonPath("$.policies").isEmpty());
    }

    @Test
    void tenantMismatchHasHttpForbiddenRatherThanInternalServerError() throws Exception {
        final MockMvc mvc = standaloneSetup(new AbacPolicyController(query ->
                Either.left(new AbacFailure("TENANT_MISMATCH", "租户不一致")))).build();
        mvc.perform(get("/internal/abac/policies").param("tenant", "t002").param("permission", "update"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void repositoryFailureDoesNotReturnASuccessfulEmptyRuleSet() throws Exception {
        final MockMvc mvc = standaloneSetup(new AbacPolicyController(query ->
                Either.left(new AbacFailure("POLICY_QUERY_FAILED", "数据库查询失败")))).build();
        mvc.perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "update"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500));
    }
}
