package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.fesod.ExcelReadCustomizer;
import com.github.zeng.alt.excel.fesod.ExcelWriteCustomizer;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import com.github.zeng.alt.i18n.MessageBaseNameProvider;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Role;
import org.springframework.core.convert.ConversionService;

/**
 * Excel 组件自动配置。
 * <p>
 * 旧版本缺 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}，
 * 两个 {@code @AutoConfiguration} 从来没有被加载过，整个模块是死代码；本次补齐。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
// 必须排在 Validator 的提供方之后：ExcelValidationConfiguration 上的 @ConditionalOnBean
// 是按自动配置的处理顺序求值的，排在前面会看不到 Validator，行校验静默失效
@AutoConfiguration(
        after = ValidationAutoConfiguration.class,
        afterName = "com.github.zeng.alt.i18n.LocaleConfiguration")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "alt.excel", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ExcelProperties.class)
@ImportRuntimeHints(ExcelRuntimeHints.class)
public class ExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FesodExcelContext fesodExcelContext(
            ExcelProperties properties,
            ObjectProvider<ExcelReadCustomizer> readCustomizers,
            ObjectProvider<ExcelWriteCustomizer> writeCustomizers,
            ObjectProvider<ExcelRowValidator> rowValidator,
            ObjectProvider<ConversionService> conversionService) {

        // 一律传 ObjectProvider、不在这里解析：ConversionService 在 Web 应用里由
        // EnableWebMvcConfiguration 提供，而它又在等本组件贡献的 WebMvcConfigurer，
        // 提前解析会形成循环依赖
        return new FesodExcelContext(properties, readCustomizers, writeCustomizers, rowValidator, conversionService);
    }

    @Bean
    @ConditionalOnMissingBean(ExcelTemplate.class)
    public ExcelTemplate excelTemplate(FesodExcelContext context) {
        return new FesodExcelTemplate(context);
    }

    @Bean
    public MessageBaseNameProvider excelMessageBaseNameProvider() {
        return () -> new String[] {"excel"};
    }

    /**
     * 行校验只在 classpath 上有 Bean Validation 且容器里确实有 {@code Validator} 时装配。
     * <p>
     * 放在嵌套配置里而不是直接写成 {@code @Bean}：{@code jakarta.validation} 是
     * {@code compileOnly} 依赖，方法签名里出现 {@code Validator} 会让缺少该依赖的
     * 应用在解析配置类时报 {@code NoClassDefFoundError}。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Validator.class)
    @ConditionalOnBean(Validator.class)
    static class ExcelValidationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ExcelRowValidator excelRowValidator(Validator validator) {
            return new ExcelRowValidator(validator);
        }
    }
}
