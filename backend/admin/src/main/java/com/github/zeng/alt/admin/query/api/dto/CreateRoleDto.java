package com.github.zeng.alt.admin.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@Schema(name = "角色信息")
public class CreateRoleDto {

    @NotEmpty(message = "角色编码不能为空")
    @Schema(name = "角色编码")
    private String code;

    @NotEmpty(message = "角色名称不能为空")
    @Schema(name = "角色名称")
    private String name;

    @Schema(name = "状态")
    private Boolean enabled = true;

    @Schema(name = "权限Id列表")
    private List<Long> permissionIds;
}
