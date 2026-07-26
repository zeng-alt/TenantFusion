package com.github.zeng.alt.admin.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Schema(name = "角色信息")
public class PatchRoleDto {

    @Schema(name = "角色名称")
    private String name;

    @Schema(name = "状态")
    private Boolean enabled;

    @Schema(name = "权限Id列表")
    private List<Long> permissionIds;
}
