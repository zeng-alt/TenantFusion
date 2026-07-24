package com.github.zeng.alt.admin.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月24日
 * @version 1.0
 */
@Data
@Validated
@Schema(name = "用户信息")
public class PatchUserDto {

    @Schema(name = "用户名")
    private String username;

    @Schema(name = "状态")
    private Boolean enabled;

    @Schema(name = "角色id列表")
    private List<Long> roleIds;
}
