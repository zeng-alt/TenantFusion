package com.github.zeng.alt.config.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 配置管理命令。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record ConfigAppCmd(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}") String appCode,
    @Size(max = 128) String appName,
    @Size(max = 512) String description
) {
}
