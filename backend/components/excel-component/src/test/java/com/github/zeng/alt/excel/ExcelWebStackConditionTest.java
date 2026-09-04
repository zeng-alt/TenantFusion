package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebFluxAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebMvcAutoConfiguration;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.web.reactive.ExcelExportResultHandler;
import com.github.zeng.alt.excel.web.reactive.ExcelImportReactiveArgumentResolver;
import com.github.zeng.alt.excel.web.servlet.ExcelExportReturnValueHandler;
import com.github.zeng.alt.excel.web.servlet.ExcelImportArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三种运行形态下 Web 集成的装配条件。
 * <p>
 * 组件可能被放进非 Web 应用、Servlet 应用或 WebFlux 应用。前者
 * {@code @ExcelImport} / {@code @ExcelExport} 就该不生效（而不是报错），
 * 后两者各装自己那套，且不能互相串。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelWebStackConditionTest {

    private static final Class<?>[] AUTO_CONFIGS = {
            ExcelAutoConfiguration.class,
            ExcelWebAutoConfiguration.class,
            ExcelWebMvcAutoConfiguration.class,
            ExcelWebFluxAutoConfiguration.class,
            ValidationAutoConfiguration.class
    };

    @Test
    void nonWebApplicationSkipsWebIntegrationEntirely() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    // 模板照常可用，注解相关的 bean 一个都不装
                    assertThat(context).hasSingleBean(ExcelTemplate.class);
                    assertThat(context).doesNotHaveBean(ExcelWebSpecFactory.class);
                    assertThat(context).doesNotHaveBean(ExcelReactiveSupport.class);
                    assertThat(context).doesNotHaveBean(ExcelImportArgumentResolver.class);
                    assertThat(context).doesNotHaveBean(ExcelImportReactiveArgumentResolver.class);
                });
    }

    @Test
    void nonWebApplicationCanStillReadAndWrite() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    excelTemplate.write(UserRow.class)
                            .to(output)
                            .i18nHead(false)
                            .write(List.of(new UserRow("张三", 18)))
                            .get();

                    assertThat(excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(output.toByteArray()))
                            .execute()
                            .rows()).hasSize(1);
                });
    }

    @Test
    void servletApplicationWiresOnlyMvcIntegration() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ExcelWebSpecFactory.class);
                    assertThat(context).hasSingleBean(ExcelImportArgumentResolver.class);
                    assertThat(context).hasSingleBean(ExcelExportReturnValueHandler.class);
                    assertThat(context).doesNotHaveBean(ExcelImportReactiveArgumentResolver.class);
                    assertThat(context).doesNotHaveBean(ExcelExportResultHandler.class);
                });
    }

    @Test
    void reactiveApplicationWiresOnlyWebFluxIntegration() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ExcelWebSpecFactory.class);
                    assertThat(context).hasSingleBean(ExcelImportReactiveArgumentResolver.class);
                    assertThat(context).hasSingleBean(ExcelExportResultHandler.class);
                    assertThat(context).doesNotHaveBean(ExcelImportArgumentResolver.class);
                    assertThat(context).doesNotHaveBean(ExcelExportReturnValueHandler.class);
                });
    }

    @Test
    void webIntegrationCanBeTurnedOffByProperty() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
                .withPropertyValues("alt.excel.web.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(ExcelTemplate.class);
                    assertThat(context).doesNotHaveBean(ExcelWebSpecFactory.class);
                    assertThat(context).doesNotHaveBean(ExcelImportArgumentResolver.class);
                });
    }

    @Test
    void reactiveApplicationWiresResultHandlerAheadOfResponseBody() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AUTO_CONFIGS))
                .withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
                .run(context -> {
                    // ResponseBodyResultHandler 的 order 是 100，本处理器必须更小
                    assertThat(context.getBean(ExcelExportResultHandler.class).getOrder()).isLessThan(100);
                });
    }
}
