package com.github.zeng.alt.i18n;

import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Locale;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月06日 20:50
 */
@AutoConfiguration(after = MessageSourceAutoConfiguration.class)
public class LocaleConfiguration {

    /**
     * 使用自定义LocalValidatorFactoryBean
     * 设置Spring国际化消息源，用户jsr303验证信息实现自定义国际化
     */
    @Bean
    public Validator getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor(
            MessageSource messageSource,
            ObjectProvider<MessageBaseNameProvider> messageBaseNameProviders) {
        ResourceBundleMessageSource resourceBundle;
        if (messageSource instanceof ResourceBundleMessageSource r) {
            resourceBundle = r;
        } else {
            resourceBundle = null;
        }

        MessageSourceAccessor accessor = null;

        if (resourceBundle != null) {
            // 延迟执行
            MessageSourceAccessor finalAccessor = accessor;
            accessor = new MessageSourceAccessor(new MessageSource() {
                @Override
                public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
                    resourceBundle.addBasenames(
                            messageBaseNameProviders.orderedStream()
                                    .map(MessageBaseNameProvider::getMessageBaseName)
                                    .toArray(String[]::new)
                    );
                    return finalAccessor.getMessage(code, args, defaultMessage, locale);
                }

                @Override
                public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
                    return finalAccessor.getMessage(code, args, locale);
                }

                @Override
                public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
                    return finalAccessor.getMessage(resolvable, locale);
                }
            });
        } else {
            accessor = new MessageSourceAccessor(messageSource);
        }

        return accessor;
    }
}
