package com.github.zeng.alt.config.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 配置管理命令。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record ConfigPublishCmd(
    @NotBlank String appCode,
    @NotEmpty List<@NotNull Long> configIds,
    String releaseNote
) {
}
