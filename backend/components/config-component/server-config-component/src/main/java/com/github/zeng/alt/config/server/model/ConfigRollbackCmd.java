package com.github.zeng.alt.config.server.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 配置管理命令。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record ConfigRollbackCmd(
    @NotBlank String appCode
) {
}
