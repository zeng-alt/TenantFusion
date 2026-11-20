package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.infrastructure.security.AbacWorkbenchService;
import com.github.zeng.alt.admin.query.api.dto.AbacPermissionVO;
import com.github.zeng.alt.admin.query.api.dto.AbacPolicyCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationVO;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.abac.method.AbacSchemaDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ABAC 编辑、字段元数据及后端模拟求值端点。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/v1/abac")
@RequiredArgsConstructor
public class AbacController {
    private final AbacWorkbenchService service;

    @GetMapping("/permissions")
    public RestResponse<List<AbacPermissionVO>> permissions() { return RestResponse.success(service.listPermissions()); }

    @GetMapping("/metadata")
    public RestResponse<AbacSchemaDto> metadata(@RequestParam String permission) {
        return RestResponse.success(service.schema(permission));
    }

    @GetMapping("/policies")
    public RestResponse<List<AbacPolicyDto>> policies(@RequestParam String permission) {
        return RestResponse.success(service.listPolicies(permission));
    }

    @PostMapping("/simulations")
    public RestResponse<AbacSimulationVO> simulate(@Valid @RequestBody AbacSimulationCmd command) {
        return RestResponse.success(service.simulate(command));
    }

    @PostMapping("/policies")
    public RestResponse<AbacPolicyDto> create(@Valid @RequestBody AbacPolicyCmd command) {
        return RestResponse.success(service.create(command).getOrElseThrow(failure -> new BaseException(400, failure.message())));
    }

    @PutMapping("/policies/{id}")
    public RestResponse<AbacPolicyDto> update(@PathVariable String id, @Valid @RequestBody AbacPolicyCmd command) {
        return RestResponse.success(service.update(id, command).getOrElseThrow(failure -> new BaseException(400, failure.message())));
    }

    @DeleteMapping("/policies/{id}")
    public RestResponse<Boolean> delete(@PathVariable String id) {
        return RestResponse.success(service.delete(id).getOrElseThrow(failure -> new BaseException(400, failure.message())));
    }

    @PostMapping("/policies/refresh")
    public RestResponse<Boolean> refresh(@RequestParam String permission) {
        return RestResponse.success(service.refresh(permission).getOrElseThrow(failure -> new BaseException(400, failure.message())));
    }
}
