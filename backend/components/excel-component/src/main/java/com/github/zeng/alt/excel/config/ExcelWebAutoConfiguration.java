package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.web.ExcelExportReturnValueHandler;
import com.github.zeng.alt.excel.web.ExcelImportArgumentResolver;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.NoOpExcelReactiveSupport;
import com.github.zeng.alt.excel.web.RxJavaExcelReactiveSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code @ExcelImport} / {@code @ExcelExport} 的 Spring MVC 集成。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, WebMvcConfigurer.class})
@ConditionalOnProperty(prefix = "alt.excel.web", name = "enabled", matchIfMissing = true)
public class ExcelWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelImportArgumentResolver excelImportArgumentResolver(
            ExcelTemplate excelTemplate, ExcelProperties properties, ExcelReactiveSupport reactiveSupport) {
        return new ExcelImportArgumentResolver(excelTemplate, properties, reactiveSupport);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelExportReturnValueHandler excelExportReturnValueHandler(
            ExcelTemplate excelTemplate, ExcelReactiveSupport reactiveSupport) {
        return new ExcelExportReturnValueHandler(excelTemplate, reactiveSupport);
    }

    /**
     * classpath 上没有 RxJava 时的兜底：响应式形状给出明确报错，集合形状照常工作。
     * <p>
     * 条件用 {@code @ConditionalOnMissingClass} 而不是靠 bean 定义的注册顺序去和
     * {@link ExcelRxJavaConfiguration} 互斥——两者按 classpath 严格二选一，
     * 不依赖 Spring 处理嵌套配置的先后。
     *
     * @return 兜底适配
     */
    @Bean
    @ConditionalOnMissingBean(ExcelReactiveSupport.class)
    @ConditionalOnMissingClass("io.reactivex.rxjava3.core.Flowable")
    public ExcelReactiveSupport noOpExcelReactiveSupport() {
        return new NoOpExcelReactiveSupport();
    }

    /**
     * classpath 上有 RxJava 时装配真正的适配实现。
     * <p>
     * 条件写成类名字符串（{@code @ConditionalOnClass(name = ...)}）而不是
     * {@code Flowable.class}：RxJava 是可选依赖，配置类里出现硬引用会让缺少它的
     * 应用在解析配置时报 {@code NoClassDefFoundError}。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.reactivex.rxjava3.core.Flowable")
    static class ExcelRxJavaConfiguration {

        @Bean
        @ConditionalOnMissingBean(ExcelReactiveSupport.class)
        ExcelReactiveSupport rxJavaExcelReactiveSupport() {
            return new RxJavaExcelReactiveSupport();
        }
    }

    /**
     * 参数解析器走标准扩展点：自定义解析器会被插在内置解析器之后、
     * 兜底的 {@code ModelAttribute} 解析器之前，正好是需要的位置。
     *
     * @param resolver 本组件的参数解析器
     * @return MVC 配置贡献者
     */
    @Bean
    public WebMvcConfigurer excelWebMvcConfigurer(ExcelImportArgumentResolver resolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(resolver);
            }
        };
    }

    /**
     * 返回值处理器必须插到「最前面」。
     * <p>
     * {@code WebMvcConfigurer#addReturnValueHandlers} 是追加到内置处理器之后的，
     * 而 {@code List<T>} 这类返回值会先被 {@code RequestResponseBodyMethodProcessor}
     * 接走（{@code @RestController} 上有隐式 {@code @ResponseBody}），本组件就永远
     * 拿不到。所以在所有单例就绪后直接改写 adapter 的处理器列表，把自己放到 0 号位。
     * <p>
     * 用 {@link SmartInitializingSingleton} + {@link ObjectProvider} 延迟取 adapter，
     * 而不是构造器注入——注入会把 MVC 适配器的创建提前到本配置类初始化时，
     * 容易和 MVC 基础设施形成循环依赖。
     *
     * @param adapter MVC 适配器，非 MVC 环境下可能不存在
     * @param handler 本组件的返回值处理器
     * @return 初始化回调
     */
    @Bean
    public SmartInitializingSingleton excelReturnValueHandlerRegistrar(
            ObjectProvider<RequestMappingHandlerAdapter> adapter, ExcelExportReturnValueHandler handler) {

        return () -> adapter.ifAvailable(target -> {
            List<HandlerMethodReturnValueHandler> current = target.getReturnValueHandlers();
            if (current == null || current.contains(handler)) {
                return;
            }
            List<HandlerMethodReturnValueHandler> reordered = new ArrayList<>(current);
            reordered.add(0, handler);
            target.setReturnValueHandlers(reordered);
        });
    }
}
