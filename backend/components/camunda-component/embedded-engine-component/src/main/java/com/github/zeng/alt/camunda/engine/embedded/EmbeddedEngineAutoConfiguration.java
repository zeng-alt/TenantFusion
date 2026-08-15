package com.github.zeng.alt.camunda.engine.embedded;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 嵌入式引擎实现自动配置
 * <p>
 * 依赖 camunda-bpm-spring-boot-starter 自动配置的引擎服务，注册全套 SPI 实现。
 *
 * @author zengAlt
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.github.zeng.alt.camunda.engine.embedded")
public class EmbeddedEngineAutoConfiguration {

}
