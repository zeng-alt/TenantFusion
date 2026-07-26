package com.github.zeng.alt.admin.query.api.dto;

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
public class AuthorizePermissionDto {

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;

    private List<Long> permissionIds;
}
