package com.github.zeng.alt.config.server.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 配置管理命令。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record ConfigUpdateCmd(
    @NotNull String content,
    @Size(max = 512) String description,
    String operator
) {
}
