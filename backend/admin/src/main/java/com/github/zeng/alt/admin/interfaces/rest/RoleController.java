package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.query.api.RoleService;
import com.github.zeng.alt.api.rest.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色用户接口")
@RestController
@RequestMapping("/v1/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "给角色添加用户")
    @PatchMapping("/users/add/{roleId}")
    public RestResponse<Void> addRoleUsers(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        List<Long> userIds = body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return RestResponse.fail("userIds不能为空");
        }
        roleService.addRoleUsers(roleId, userIds);
        return RestResponse.success();
    }

    @Operation(summary = "从角色移除用户")
    @PatchMapping("/users/remove/{roleId}")
    public RestResponse<Void> removeRoleUsers(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        List<Long> userIds = body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return RestResponse.fail("userIds不能为空");
        }

        roleService.removeRoleUsers(roleId, userIds);
        return RestResponse.success();
    }
}
