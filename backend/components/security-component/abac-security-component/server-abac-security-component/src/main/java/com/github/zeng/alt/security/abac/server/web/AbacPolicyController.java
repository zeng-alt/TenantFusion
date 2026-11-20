package com.github.zeng.alt.security.abac.server.web;

import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 策略查询端点，继续受宿主认证、授权及权威存储租户校验保护。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RestController
@RequiredArgsConstructor
public class AbacPolicyController {
    private final AbacPolicyQueryService queries;

    @GetMapping(AbacPolicyQueryService.PATH)
    public ResponseEntity<?> get(@RequestParam String tenant, @RequestParam String permission) {
        return queries.get(new AbacPolicyQuery(tenant, permission)).fold(failure -> {
            final HttpStatus status = "POLICY_SCOPE_MISMATCH".equals(failure.code()) || "TENANT_MISMATCH".equals(failure.code())
                    ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, failure.message()));
        }, ResponseEntity::ok);
    }
}
