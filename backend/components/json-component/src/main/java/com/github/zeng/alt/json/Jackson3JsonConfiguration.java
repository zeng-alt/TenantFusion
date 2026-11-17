package com.github.zeng.alt.json;

import com.github.zeng.alt.json.jackson3.BusinessJacksonModule;
import com.github.zeng.alt.json.jackson3.VavrJacksonModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 Boot 4 默认的 Jackson 3 映射器提供组件扩展。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
public class Jackson3JsonConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public BusinessJacksonModule businessJackson3Module() {
        return new BusinessJacksonModule();
    }

    @Bean
    @ConditionalOnMissingBean
    public VavrJacksonModule vavrJackson3Module() {
        return new VavrJacksonModule();
    }
}
