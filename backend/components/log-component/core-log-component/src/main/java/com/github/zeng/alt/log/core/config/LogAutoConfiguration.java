package com.github.zeng.alt.log.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.Log;
import com.github.zeng.alt.log.core.aot.LogRuntimeHints;
import com.github.zeng.alt.log.core.handler.DefaultLogHandler;
import com.github.zeng.alt.log.core.handler.DefaultLogRecordFactory;
import com.github.zeng.alt.log.core.handler.LogHandler;
import com.github.zeng.alt.log.core.handler.LogRecordFactory;
import com.github.zeng.alt.log.core.interceptor.LogMethodInterceptor;
import com.github.zeng.alt.log.core.operation.AnnotationLogOperationSource;
import com.github.zeng.alt.log.core.operation.LogOperationSource;
import com.github.zeng.alt.log.core.support.*;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 日志模块自动配置。
 * <p>
 * Log module auto-configuration.
 * <p>
 * 装配 {@link Log} 注解的 AOP 拦截、日志记录工厂、处理器及支持组件。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ImportRuntimeHints(LogRuntimeHints.class)
public class LogAutoConfiguration {

    // ========== AOP 拦截 ==========

    @Bean
    public Advisor logAdvisor(LogHandler handler, LogOperationSource source) {
        return new LogMethodInterceptor(
                handler,
                source,
                new AnnotationMatchingPointcut(null, Log.class, true),
                10
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public LogOperationSource annotationLogOperationSource() {
        return new AnnotationLogOperationSource();
    }

    // ========== 日志处理器 ==========

    @Bean
    @ConditionalOnMissingBean(LogHandler.class)
    public LogHandler defaultLogHandler(
            LogRecordFactory factory,
            ApplicationEventPublisher publisher) {
        return new DefaultLogHandler(factory, publisher);
    }

    @Bean
    @ConditionalOnMissingBean(LogRecordFactory.class)
    public LogRecordFactory defaultLogRecordFactory(
            UserResolver userResolver,
            IpResolver ipResolver,
            RequestResolver requestResolver,
            RequestParameterResolver parameterResolver,
            ObjectMapper objectMapper) {
        return new DefaultLogRecordFactory(
                userResolver, ipResolver,
                requestResolver, parameterResolver,
                objectMapper);
    }

    // ========== 支持组件（Web 环境） ==========

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(IpResolver.class)
    public IpResolver servletIpResolver() {
        return new ServletIpResolver();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(RequestResolver.class)
    public RequestResolver servletRequestResolver() {
        return new ServletRequestResolver();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(RequestParameterResolver.class)
    public RequestParameterResolver defaultRequestParameterResolver(ObjectMapper objectMapper) {
        return new DefaultRequestParameterResolver(objectMapper);
    }

    // ========== 用户解析器（需要 security-api） ==========

    @Bean
    @ConditionalOnClass(name = "com.github.zeng.alt.security.api.UserContextHolder")
    @ConditionalOnMissingBean(UserResolver.class)
    public UserResolver defaultUserResolver() {
        return new DefaultUserResolver();
    }
}
