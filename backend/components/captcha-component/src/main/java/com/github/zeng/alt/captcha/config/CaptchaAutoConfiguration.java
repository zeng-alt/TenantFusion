package com.github.zeng.alt.captcha.config;

import com.github.zeng.alt.captcha.CaptchaRuntimeHints;
import com.github.zeng.alt.captcha.core.CaptchaRenderer;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.core.CaptchaTemplateImpl;
import com.github.zeng.alt.captcha.producer.ArithmeticCaptchaProducer;
import com.github.zeng.alt.captcha.producer.CaptchaProducer;
import com.github.zeng.alt.captcha.producer.RandomCodeProducer;
import com.github.zeng.alt.storage.StorageTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.List;

@AutoConfiguration
@ImportRuntimeHints(CaptchaRuntimeHints.class)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    @Bean
    public CaptchaRenderer captchaRenderer(CaptchaProperties properties) {
        return new CaptchaRenderer(
                properties.getImage().getWidth(),
                properties.getImage().getHeight()
        );
    }

    @Bean
    public CaptchaProducer randomCodeProducer(CaptchaRenderer renderer, CaptchaProperties properties) {
        return new RandomCodeProducer(renderer, properties.getCode().getLength());
    }

    @Bean
    public CaptchaProducer arithmeticCaptchaProducer(CaptchaRenderer renderer) {
        return new ArithmeticCaptchaProducer(renderer);
    }

    @Bean
    @ConditionalOnMissingBean(CaptchaTemplate.class)
    public CaptchaTemplate captchaTemplate(StorageTemplate storageTemplate,
                                           CaptchaProperties properties,
                                           List<CaptchaProducer> producers) {
        return new CaptchaTemplateImpl(storageTemplate, properties, producers);
    }
}
