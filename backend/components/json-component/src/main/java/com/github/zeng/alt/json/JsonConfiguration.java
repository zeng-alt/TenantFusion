package com.github.zeng.alt.json;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Role;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IDictTranslateService;
import com.github.zeng.alt.json.spi.IEncryptService;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@Import(Jackson3JsonConfiguration.class)
@ImportRuntimeHints(JsonRuntimeHints.class)
@PropertySource("classpath:alt-json-defaults.properties")
public class JsonConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JacksonHelper jsonHelper(ObjectProvider<JsonMapper> provider) {
        return new JacksonHelper(provider);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static DictServiceHolder dictServiceHolder(ObjectProvider<IDictTranslateService> provider) {
        provider.ifAvailable(DictServiceHolder::setService);
        return new DictServiceHolder();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static EncryptServiceHolder encryptServiceHolder(ObjectProvider<IEncryptService> provider) {
        provider.ifAvailable(EncryptServiceHolder::setService);
        return new EncryptServiceHolder();
    }

}
