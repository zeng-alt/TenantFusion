package com.github.zeng.alt.doc;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
@AutoConfiguration(before = SpringDocConfiguration.class)
@EnableConfigurationProperties(SpringDocProperties.class)
public class SpringDocAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI(SpringDocProperties properties) {
        return new OpenApiFactory().build(properties);
    }
}