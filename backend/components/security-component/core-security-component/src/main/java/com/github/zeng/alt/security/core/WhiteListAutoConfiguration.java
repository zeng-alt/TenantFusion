package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.HttpWhiteListProperties;
import com.github.zeng.alt.security.core.properties.WhiteListProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 装配统一白名单，代码贡献先于组件配置默认规则应用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableConfigurationProperties({WhiteListProperties.class, HttpWhiteListProperties.class})
public class WhiteListAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public WhiteListService defaultWhiteListService(WhiteListProperties legacy, HttpWhiteListProperties properties) {
        return new WhiteListServiceImpl(legacy, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WhiteListTemplate whiteListTemplate(
            WhiteListService service,
            ObjectProvider<WhiteListCustomizer> customizers
    ) {
        final var template = new WhiteListTemplate(service);
        customizers.orderedStream().forEach(customizer -> customizer.customize(template));
        return template.refresh();
    }
}
