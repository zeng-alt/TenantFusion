package com.github.zeng.alt.bean.config;

import com.github.zeng.alt.bean.ApplicationContextHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

@AutoConfiguration
@ImportRuntimeHints(BeanHelperRuntimeHints.class)
public class BeanAutoConfiguration {

    @Bean
    public static ApplicationContextHelper applicationContextHelper() {
        return new ApplicationContextHelper();
    }
}
