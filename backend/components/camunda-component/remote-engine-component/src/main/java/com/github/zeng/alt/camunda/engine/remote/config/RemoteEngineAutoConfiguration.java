package com.github.zeng.alt.camunda.engine.remote.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import feign.Logger;
import feign.Retryer;
import feign.codec.Encoder;
import java.time.OffsetDateTime;
import java.util.Date;
import org.camunda.community.rest.client.KeyChangingSpringManyMultipartFilesWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
// 上游 EnableCamundaFeignClients 会导入硬编码 Jackson 2 的转换器；在此使用相同的客户端与 multipart 编码器。
@EnableFeignClients(basePackages = "org.camunda.community.rest.client.api")
@ComponentScan(basePackages = "com.github.zeng.alt.camunda.engine.remote")
public class RemoteEngineAutoConfiguration {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Bean
    @ConditionalOnMissingBean
    public Encoder camundaFeignEncoder(
            FeignEncoderProperties properties,
            ObjectProvider<FeignHttpMessageConverters> converters
    ) {
        return new SpringEncoder(KeyChangingSpringManyMultipartFilesWriter.camundaMultipartFormEncoder(),
                properties, converters);
    }

    @Bean
    @ConditionalOnMissingBean
    public Retryer camundaRetryer() {
        return new Retryer.Default();
    }

    @Bean
    @ConditionalOnProperty("feign.client.config.default.loggerLevel")
    public Logger.Level feignLoggerLevel(Environment environment) {
        return environment.getRequiredProperty("feign.client.config.default.loggerLevel", Logger.Level.class);
    }

    @Bean
    public ClientHttpMessageConvertersCustomizer camunda7ClientHttpMessageConvertersCustomizer(
            ObjectProvider<JsonMapper> mappers
    ) {
        final JsonMapper mapper = mappers.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build())
                .rebuild()
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .withConfigOverride(Date.class, config -> config.setFormat(JsonFormat.Value.forPattern(DATE_PATTERN)))
                .withConfigOverride(OffsetDateTime.class,
                        config -> config.setFormat(JsonFormat.Value.forPattern(DATE_PATTERN)))
                .build();
        return converters -> converters.addCustomConverter(new JacksonJsonHttpMessageConverter(mapper));
    }
}
