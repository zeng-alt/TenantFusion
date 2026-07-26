package com.github.zeng.alt.admin.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * @author zengJiaJun
 * @since 2026年07月24日
 * @version 1.0
 */
@Data
@Schema(name = "用户信息")
public class ResetUserPasswordDto {

    @NotEmpty(message = "密码不能为空")
    @Schema(name = "密码")
    private String password;
}
