package com.github.zeng.alt.security.abac.spel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 已解析表达式的有界缓存；不启用运行时字节码生成，兼容现有限制和 Native 部署。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@ConfigurationProperties("security.abac.spel")
public class AbacSpelProperties {
    private long maximumSize = 10000;
    private Duration cacheTtl = Duration.ofMinutes(30);
}
