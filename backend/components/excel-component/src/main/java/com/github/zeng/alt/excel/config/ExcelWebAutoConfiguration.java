package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.web.NoOpExcelReactiveSupport;
import com.github.zeng.alt.excel.web.RxJavaExcelReactiveSupport;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Web 集成的共享装配：Servlet 与 WebFlux 两条集成都要的 bean 放这里。
 * <p>
 * 三种运行形态：
 * <ul>
 *   <li><b>非 Web 应用</b>——本配置整体不生效，{@code @ExcelImport} /
 *       {@code @ExcelExport} 自然不起作用，{@code ExcelTemplate} 照常可用。</li>
 *   <li><b>Servlet（WebMVC）</b>——{@link ExcelWebMvcAutoConfiguration} 生效。</li>
 *   <li><b>WebFlux</b>——{@link ExcelWebFluxAutoConfiguration} 生效。</li>
 * </ul>
 * {@code @ConditionalOnWebApplication} 不带 {@code type}，两种 Web 栈都满足；
 * 具体栈的判断留给上面两个配置。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "alt.excel.web", name = "enabled", matchIfMissing = true)
public class ExcelWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelWebSpecFactory excelWebSpecFactory(ExcelTemplate excelTemplate) {
        return new ExcelWebSpecFactory(excelTemplate);
    }

    /**
     * classpath 上没有 RxJava 时的兜底：{@code Flowable} 形状给出明确报错，
     * 其余形状照常工作。
     * <p>
     * 条件用 {@code @ConditionalOnMissingClass} 而不是靠 bean 定义的注册顺序去和
     * {@link ExcelRxJavaConfiguration} 互斥——两者按 classpath 严格二选一。
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
     * 条件写成类名字符串而不是 {@code Flowable.class}：RxJava 是可选依赖，
     * 配置类里出现硬引用会让缺少它的应用在解析配置时报 {@code NoClassDefFoundError}。
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
}
