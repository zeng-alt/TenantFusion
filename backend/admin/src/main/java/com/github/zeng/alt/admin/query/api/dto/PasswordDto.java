package com.github.zeng.alt.admin.query.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordDto {
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
}