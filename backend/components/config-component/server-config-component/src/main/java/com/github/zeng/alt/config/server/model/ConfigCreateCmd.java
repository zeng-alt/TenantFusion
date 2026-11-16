package com.github.zeng.alt.config.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 配置管理命令。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record ConfigCreateCmd(
    @NotBlank String appCode,
    @NotBlank @Size(max = 255) String dataId,
    @Size(min = 1, max = 128) @Pattern(regexp = "(?s).*\\S.*") String group,
    @NotNull String content,
    @Size(max = 32) String format,
    @Size(max = 512) String description
) {
}
