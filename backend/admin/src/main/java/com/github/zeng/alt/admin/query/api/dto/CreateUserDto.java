package com.github.zeng.alt.admin.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月24日
 * @version 1.0
 */
@Data
@Schema(name = "用户信息")
public class CreateUserDto {

    @NotEmpty(message = "用户名不能为空")
    @Schema(name = "用户名")
    private String username;

    @NotEmpty(message = "密码不能为空")
    @Schema(name = "密码")
    private String password;

    @Schema(name = "状态")
    private Boolean enabled = true;

    @Schema(name = "部门ID")
    private Long deptId;

    @Schema(name = "角色id列表")
    private List<Long> roleIds;
}
