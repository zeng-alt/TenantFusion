package com.github.zeng.alt.security.abac.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

/**
 * 策略服务端开关及可选的跨进程通知绑定。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@ConfigurationProperties("security.abac.server")
public class AbacServerProperties {
    private boolean enabled;
    private boolean notificationsEnabled;
    private String notificationBinding = "abac-policies";
    private Map<String, AbacServiceClientProperties> clients = Map.of();
}
