package com.github.zeng.alt.security.abac.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * ABAC 核心配置属性。执行门面由客户端装配，纯服务端依赖 core 不会创建执行链。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableConfigurationProperties(AbacProperties.class)
public class AbacAutoConfiguration {
}
