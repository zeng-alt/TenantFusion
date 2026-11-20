package com.github.zeng.alt.security.abac.client.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ABAC 客户端的规则来源、内存缓存和通知配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@ConfigurationProperties("security.abac.client")
public class AbacClientProperties {
    private boolean enabled = true;
    private Source source = Source.LOCAL;
    private String serviceId = "admin-application";
    private String clientId = "";
    @ToString.Exclude
    private String serviceKey = "";
    private long maximumSize = 10000;
    private Duration cacheTtl = Duration.ofMinutes(5);
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean notificationsEnabled;
    private String notificationBinding = "abac-policies";

    /**
     * 权威查询来源。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public enum Source {
        LOCAL, REMOTE
    }
}
