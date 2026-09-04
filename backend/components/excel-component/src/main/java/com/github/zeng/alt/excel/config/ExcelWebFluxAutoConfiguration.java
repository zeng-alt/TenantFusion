package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.web.reactive.ExcelExportResultHandler;
import com.github.zeng.alt.excel.web.reactive.ExcelImportReactiveArgumentResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * {@code @ExcelImport} / {@code @ExcelExport} 的 Spring WebFlux 集成。
 * <p>
 * 只在响应式 Web 应用里生效；Servlet 应用走 {@link ExcelWebMvcAutoConfiguration}，
 * 非 Web 应用两个都不生效。
 * <p>
 * 与 Servlet 集成的差别在两处，都由 WebFlux 的模型决定：
 * <ul>
 *   <li>返回值处理器实现 {@code HandlerResultHandler} 并用 {@code Ordered} 抢在
 *       {@code ResponseBodyResultHandler}（order 100）之前，不需要改写任何内置列表。</li>
 *   <li>Excel 的读写是阻塞动作，全部放到 {@code Schedulers.boundedElastic()} 上，
 *       不占事件循环。</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@AutoConfiguration(after = {WebFluxAutoConfiguration.class, ExcelWebAutoConfiguration.class})
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({DispatcherHandler.class, WebFluxConfigurer.class})
@ConditionalOnProperty(prefix = "alt.excel.web", name = "enabled", matchIfMissing = true)
public class ExcelWebFluxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelImportReactiveArgumentResolver excelImportReactiveArgumentResolver(
            ExcelWebSpecFactory specFactory, ExcelProperties properties, ExcelReactiveSupport reactiveSupport) {
        return new ExcelImportReactiveArgumentResolver(specFactory, properties, reactiveSupport);
    }

    /**
     * 返回值处理器是普通 bean 即可：{@code DispatcherHandler} 会把容器里所有
     * {@code HandlerResultHandler} 收集起来并按 {@code Ordered} 排序。
     *
     * @param specFactory     读写链工厂
     * @param reactiveSupport RxJava 适配
     * @return 结果处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public ExcelExportResultHandler excelExportResultHandler(
            ExcelWebSpecFactory specFactory, ExcelReactiveSupport reactiveSupport) {
        return new ExcelExportResultHandler(specFactory, reactiveSupport);
    }

    /**
     * 参数解析器通过 {@code WebFluxConfigurer} 的自定义解析器扩展点注册。
     *
     * @param resolver 本组件的参数解析器
     * @return WebFlux 配置贡献者
     */
    @Bean
    public WebFluxConfigurer excelWebFluxConfigurer(ExcelImportReactiveArgumentResolver resolver) {
        return new WebFluxConfigurer() {
            @Override
            public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                configurer.addCustomResolver(resolver);
            }
        };
    }
}
