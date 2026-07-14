package com.github.zeng.alt.security.captcha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(CaptchaAuthProperties.class)
public class CaptchaAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.captcha-auth", name = "validation", havingValue = "true")
    public SecurityBuilderCustomizer captchaSecurityCustomizer(
            CaptchaTemplate captchaTemplate,
            CaptchaAuthProperties properties,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        return http -> {
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
            CaptchaAuthenticationFilter filter = new CaptchaAuthenticationFilter(
                    captchaTemplate, properties, objectMapper);
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        };
    }
}
