package com.github.zeng.alt.core.advice;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.annotation.Order;

/**
 * @author zengJiaJun
 * @since 2026年06月30日
 * @version 1.0
 */
@AutoConfiguration
public class CoreConfig {

//    @Bean
//    @Primary
//    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
//    public CustomErrorController customErrorController() {
//        return new CustomErrorController();
//    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CustomErrorAttributes customErrorAttributes() {
        return new CustomErrorAttributes();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GlobalServletExceptionAdvice globalServletExceptionAdvice() {
        return new GlobalServletExceptionAdvice();
    }

    @Bean
//    @ConditionalOnBean(MessageSourceAccessor.class)
    public GlobalExceptionAdvice globalExceptionAdvice(ObjectProvider<MessageSourceAccessor> provider) {
        return new GlobalExceptionAdvice(provider);
    }
}
