package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.fesod.ExcelReadCustomizer;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import com.github.zeng.alt.i18n.MessageBaseNameProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置装配测试。
 * <p>
 * 重点覆盖旧版本的两个硬伤：{@code AutoConfiguration.imports} 缺失导致组件从未装配，
 * 以及配置前缀不在项目自己的根命名空间下。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExcelAutoConfiguration.class, ValidationAutoConfiguration.class));

    @Test
    void registersTemplateAndRowValidatorByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ExcelTemplate.class);
            assertThat(context.getBean(ExcelTemplate.class)).isInstanceOf(FesodExcelTemplate.class);
            assertThat(context).hasSingleBean(ExcelRowValidator.class);
            assertThat(context).hasSingleBean(MessageBaseNameProvider.class);
        });
    }

    @Test
    void isDiscoverableThroughSpringFactoriesFile() {
        // 旧版本没有这个文件，@AutoConfiguration 从未被加载，整个模块是死代码
        assertThat(getClass().getClassLoader().getResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"))
                .isNotNull();
    }

    @Test
    void skipsRowValidatorWithoutValidatorAndStillReads() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ExcelAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ExcelTemplate.class);
                    assertThat(context).doesNotHaveBean(ExcelRowValidator.class);
                    assertThat(readBack(context.getBean(ExcelTemplate.class))).hasSize(1);
                });
    }

    @Test
    void bindsPropertiesUnderAltExcelPrefix() {
        runner.withPropertyValues(
                        "alt.excel.read.head-row-number=3",
                        "alt.excel.read.on-error=fail_fast",
                        "alt.excel.write.auto-width=false",
                        "alt.excel.write.batch-size=500")
                .run(context -> {
                    ExcelProperties properties = context.getBean(ExcelProperties.class);
                    assertThat(properties.getRead().getHeadRowNumber()).isEqualTo(3);
                    assertThat(properties.getRead().getOnError()).isEqualTo(ExcelErrorPolicy.FAIL_FAST);
                    assertThat(properties.getWrite().isAutoWidth()).isFalse();
                    assertThat(properties.getWrite().getBatchSize()).isEqualTo(500);
                });
    }

    @Test
    void appliesCustomizersInOrderContributorsBeforeDefaults() {
        runner.withUserConfiguration(CustomizerConfiguration.class).run(context -> {
            CustomizerRecorder recorder = context.getBean(CustomizerRecorder.class);
            readBack(context.getBean(ExcelTemplate.class));
            assertThat(recorder.applied).containsExactly("first", "second");
        });
    }

    @Test
    void letsCustomExcelTemplateOverrideDefault() {
        ExcelTemplate custom = Mockito.mock(ExcelTemplate.class);
        runner.withBean(ExcelTemplate.class, () -> custom)
                .run(context -> assertThat(context.getBean(ExcelTemplate.class)).isSameAs(custom));
    }

    private static List<UserRow> readBack(ExcelTemplate excelTemplate) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .write(List.of(new UserRow("张三", 18)))
                .get();
        return excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(output.toByteArray()))
                .execute()
                .rows();
    }

    /**
     * 记录 customizer 的应用顺序。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    static class CustomizerRecorder {

        private final List<String> applied = new ArrayList<>();
    }

    /**
     * 两个带 {@code @Order} 的 customizer，用于验证收集顺序。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomizerConfiguration {

        @Bean
        CustomizerRecorder customizerRecorder() {
            return new CustomizerRecorder();
        }

        @Bean
        @Order(1)
        ExcelReadCustomizer secondReadCustomizer(CustomizerRecorder recorder) {
            return builder -> recorder.applied.add("second");
        }

        @Bean
        @Order(0)
        ExcelReadCustomizer firstReadCustomizer(CustomizerRecorder recorder) {
            return builder -> recorder.applied.add("first");
        }
    }
}
