package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.web.ExcelExportReturnValueHandler;
import com.github.zeng.alt.excel.web.ExcelImportArgumentResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
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
            ExcelTemplate excelTemplate, ExcelProperties properties) {
        return new ExcelImportArgumentResolver(excelTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelExportReturnValueHandler excelExportReturnValueHandler(ExcelTemplate excelTemplate) {
        return new ExcelExportReturnValueHandler(excelTemplate);
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
