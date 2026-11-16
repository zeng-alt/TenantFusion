package com.github.zeng.alt.config.client;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * 共享 Spring Boot 属性名前缀匹配规则。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class ConfigPropertiesHelper {
    private ConfigPropertiesHelper() {
    }

    static boolean matches(String prefix, String key) {
        final ConfigurationPropertyName namespace = ConfigurationPropertyName.of(prefix);
        final ConfigurationPropertyName property = ConfigurationPropertyName.adapt(key, '.');
        return namespace.equals(property) || namespace.isAncestorOf(property);
    }
}
