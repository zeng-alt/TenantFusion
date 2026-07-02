package com.github.zeng.alt.oss.core.config;

import com.github.zeng.alt.oss.OssConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * OSS 配置刷新自动配置（Spring Cloud Config / Nacos / Apollo 集成）。
 * <p>
 * 当类路径中存在 {@code org.springframework.cloud.context.environment.EnvironmentChangeEvent}
 * （即引入了 {@code spring-cloud-context}）时自动激活。
 * <p>
 * 监听配置中心的 {@code oss.s3.*} 属性变更事件，自动触发 OSS 连接刷新。
 * <p>
 * 需要手动触发时，可使用 {@code POST /api/oss/refresh} 端点（启用 {@code oss.s3.management.enabled=true}）。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.context.environment.EnvironmentChangeEvent")
@ConditionalOnBean(OssConnectionManager.class)
@ImportRuntimeHints(OssConfigRefreshRuntimeHints.class)
public class OssConfigRefreshAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OssConfigRefreshAutoConfiguration.class);

    private static final String ENV_CHANGE_EVENT_CLASS =
            "org.springframework.cloud.context.environment.EnvironmentChangeEvent";
    private static final String OSS_PROPERTY_PREFIX = "oss.s3.";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    public ApplicationListener ossEnvironmentChangeListener(OssConnectionManager connectionManager) {
        return event -> {
            // 快速过滤：只处理 EnvironmentChangeEvent（通过类名比较避免编译期依赖）
            if (!ENV_CHANGE_EVENT_CLASS.equals(event.getClass().getName())) {
                return;
            }
            try {
                // 反射获取 event.getKeys()，这是 EnvironmentChangeEvent 特有的方法
                var keysMethod = event.getClass().getMethod("getKeys");
                var keys = (java.util.Set<String>) keysMethod.invoke(event);

                var ossKeys = keys.stream()
                        .filter(k -> k.startsWith(OSS_PROPERTY_PREFIX))
                        .toList();

                if (!ossKeys.isEmpty()) {
                    log.info("OSS configuration changed (keys: {}), triggering connection refresh...", ossKeys);
                    connectionManager.refresh();
                }
            } catch (Exception e) {
                log.warn("Failed to process EnvironmentChangeEvent", e);
            }
        };
    }
}
