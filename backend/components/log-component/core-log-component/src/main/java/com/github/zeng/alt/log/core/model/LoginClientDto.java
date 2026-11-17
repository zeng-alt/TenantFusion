package com.github.zeng.alt.log.core.model;

import java.util.Objects;

/**
 * 登录客户端名称和版本；不可识别的字段使用空字符串。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record LoginClientDto(
        String browser,
        String os
) {
    public LoginClientDto {
        Objects.requireNonNull(browser);
        Objects.requireNonNull(os);
    }
}
