package com.github.zeng.alt.log.core.config;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 操作日志中默认排除的字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties("alt.log")
public class LogProperties {
    private Set<String> excludedFields = Set.of(
            "password",
            "oldPassword",
            "newPassword",
            "confirmPassword",
            "token",
            "accessToken",
            "refreshToken",
            "authorization",
            "secret"
    );
}
