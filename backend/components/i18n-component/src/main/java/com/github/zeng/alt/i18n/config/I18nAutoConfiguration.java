package com.github.zeng.alt.i18n.config;

import com.github.zeng.alt.i18n.core.DatabaseI18nMessageService;
import com.github.zeng.alt.i18n.core.DatabaseMessageSource;
import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.core.ResourceI18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import com.github.zeng.alt.i18n.repository.I18nMessageRepository;
import com.github.zeng.alt.i18n.rest.I18nMvcHandler;
import com.github.zeng.alt.i18n.rest.I18nWebFluxHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 国际化组件唯一自动配置
 * <p>
 * 统一管理所有 Bean 的创建，根据配置和 classpath 条件按需激活：
 * <ul>
 *   <li>{@code alt.i18n.mode=file}（默认）→ 资源文件模式</li>
 *   <li>{@code alt.i18n.mode=database} → 数据库模式 + JPA 扫描</li>
 *   <li>classpath 含 {@code DispatcherServlet} → 注册 MVC 路由</li>
 *   <li>classpath 含 {@code DispatcherHandler} → 注册 WebFlux 路由</li>
 * </ul>
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
@AutoConfiguration(before = MessageSourceAutoConfiguration.class)
@EnableConfigurationProperties(I18nProperties.class)
public class I18nAutoConfiguration {

    // ==================== 文件模式 ====================

    @Bean(name = "messageSource")
    @ConditionalOnProperty(prefix = "alt.i18n", name = "mode", havingValue = "file", matchIfMissing = true)
    public MessageSource resourceBundleMessageSource(I18nProperties properties) {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename(properties.getBasename());
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    @Bean
    @ConditionalOnProperty(prefix = "alt.i18n", name = "mode", havingValue = "file", matchIfMissing = true)
    public ResourceI18nMessageService resourceI18nMessageService(
            MessageSource messageSource, I18nProperties properties) {
        return new ResourceI18nMessageService(messageSource, properties.getBasename());
    }

    // ==================== 数据库模式（JPA） ====================

    @Configuration
    @ConditionalOnProperty(prefix = "alt.i18n", name = "mode", havingValue = "database")
    @EntityScan(basePackageClasses = I18nMessageDO.class)
    @EnableJpaRepositories(basePackageClasses = I18nMessageRepository.class)
    static class I18nJpaConfig {

        @Bean(name = "messageSource")
        public MessageSource databaseMessageSource(I18nMessageRepository repo) {
            return new DatabaseMessageSource(repo);
        }

        @Bean
        public DatabaseI18nMessageService databaseI18nMessageService(
                I18nMessageRepository repo, MessageSource messageSource) {
            return new DatabaseI18nMessageService(repo, messageSource);
        }
    }

    // ==================== MVC 路由（Servlet 栈） ====================

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    static class I18nMvcRouterConfig {

        @Bean
        public I18nMvcHandler i18nMvcHandler(I18nMessageService service) {
            return new I18nMvcHandler(service);
        }

        @Bean
        public org.springframework.web.servlet.function.RouterFunction<
                org.springframework.web.servlet.function.ServerResponse> i18nMvcRouter(
                I18nMvcHandler handler, I18nProperties properties) {
            String p = properties.getPrefix();
            return org.springframework.web.servlet.function.RouterFunctions.route()
                    .GET(p, handler::findAll)
                    .GET(p + "/{code}/{locale}", handler::findByCodeAndLocale)
                    .GET(p + "/code/{code}", handler::findByCode)
                    .GET(p + "/locale/{locale}", handler::findByLocale)
                    .POST(p, handler::create)
                    .PUT(p, handler::update)
                    .DELETE(p + "/id/{id}", handler::deleteById)
                    .DELETE(p + "/{code}/{locale}", handler::deleteByCodeAndLocale)
                    .build();
        }
    }

    // ==================== WebFlux 路由（Reactive 栈） ====================

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.reactive.DispatcherHandler")
    static class I18nWebFluxRouterConfig {

        @Bean
        public I18nWebFluxHandler i18nWebFluxHandler(I18nMessageService service) {
            return new I18nWebFluxHandler(service);
        }

        @Bean
        public org.springframework.web.reactive.function.server.RouterFunction<
                org.springframework.web.reactive.function.server.ServerResponse> i18nWebFluxRouter(
                I18nWebFluxHandler handler, I18nProperties properties) {
            String p = properties.getPrefix();
            return org.springframework.web.reactive.function.server.RouterFunctions.route()
                    .GET(p, handler::findAll)
                    .GET(p + "/{code}/{locale}", handler::findByCodeAndLocale)
                    .GET(p + "/code/{code}", handler::findByCode)
                    .GET(p + "/locale/{locale}", handler::findByLocale)
                    .POST(p, handler::create)
                    .PUT(p, handler::update)
                    .DELETE(p + "/id/{id}", handler::deleteById)
                    .DELETE(p + "/{code}/{locale}", handler::deleteByCodeAndLocale)
                    .build();
        }
    }
}