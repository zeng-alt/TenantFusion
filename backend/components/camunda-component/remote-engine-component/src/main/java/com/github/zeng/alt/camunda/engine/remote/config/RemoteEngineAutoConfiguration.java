package com.github.zeng.alt.camunda.engine.remote.config;

import org.camunda.community.rest.client.EnableCamundaFeignClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 远程引擎实现自动配置
 * <p>
 * 启用 Camunda 社区 REST 客户端的 feign 客户端并扫描 SPI 实现。
 * <p>
 * 配置项：
 * <ul>
 *     <li>{@code feign.client.config.default.url}：远程引擎 REST 地址（默认 /engine-rest）</li>
 *     <li>{@code camundaplatformrest.security.basicAuth.username/password}：basic auth</li>
 * </ul>
 *
 * @author zengAlt
 */
@AutoConfiguration
@EnableCamundaFeignClients
@EnableFeignClients(basePackages = "org.camunda.community.rest.client.api")
@ComponentScan(basePackages = "com.github.zeng.alt.camunda.engine.remote")
public class RemoteEngineAutoConfiguration {

}
