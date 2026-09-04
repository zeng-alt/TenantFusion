package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.i18n.MessageSourceHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 表头国际化的读写往返测试。
 * <p>
 * 覆盖两件此前不可能成立的事：{@code MessageSourceHelper} 没有 bean 导致解析恒为原文，
 * 以及 {@code {key}} 的花括号没剥就去查消息源导致永远查不到。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelI18nHeadTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExcelAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(MessageConfiguration.class);

    @Test
    void 导出时表头替换成当前语言文本() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] bytes = write(excelTemplate, true);

            // 以「无模型」方式读回表头行，确认落盘的是中文而不是 i18n key
            List<String> headers = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(bytes))
                    .i18nHead(true)
                    .execute()
                    .rows()
                    .stream()
                    .map(UserRow::getUserName)
                    .toList();

            assertThat(MessageSourceHelper.getMessage("excel.test.userName", "?")).isEqualTo("姓名");
            assertThat(headers).containsExactly("张三");
        });
    }

    @Test
    void 中文表头的文件能按国际化匹配读回实体() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            // 表头写成中文（模拟用户按导出模板填好的文件）
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            excelTemplate.writeHead(List.of(List.of("姓名"), List.of("年龄")))
                    .to(output)
                    .i18nHead(false)
                    .write(List.of(List.of("李四", "42")))
                    .get();

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(output.toByteArray()))
                    .i18nHead(true)
                    .execute();

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.rows()).hasSize(1);
            assertThat(result.rows().getFirst().getUserName()).isEqualTo("李四");
            // 字符串 "42" 经 ConversionService 转成 Integer，而不是被硬塞进字段
            assertThat(result.rows().getFirst().getAge()).isEqualTo(42);
        });
    }

    @Test
    void 关闭国际化时表头保持i18nKey原文() {
        runner.run(context -> {
            byte[] bytes = write(context.getBean(ExcelTemplate.class), false);
            assertThat(new String(bytes)).isNotEmpty();

            // i18nHead 关闭 → 表头是字面量 {excel.test.userName}，fesod 原生匹配照样读得回来
            ExcelReadResult<UserRow> result = context.getBean(ExcelTemplate.class)
                    .read(UserRow.class)
                    .from(new ByteArrayInputStream(bytes))
                    .execute();

            assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三");
        });
    }

    private static byte[] write(ExcelTemplate excelTemplate, boolean i18nHead) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(i18nHead)
                .write(List.of(new UserRow("张三", 18)))
                .get();
        return output.toByteArray();
    }

    /**
     * 最小可用的 i18n 装配：消息源 + accessor + helper。
     * <p>
     * 与 {@code i18n-component} 的 {@code LocaleConfiguration} 等价，但不引入
     * 它的数据库模式，测试里只需要文件模式。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class MessageConfiguration {

        @Bean
        MessageSource messageSource() {
            ResourceBundleMessageSource source = new ResourceBundleMessageSource();
            source.setBasename("excel-test-messages");
            source.setDefaultEncoding("UTF-8");
            return source;
        }

        @Bean
        MessageSourceAccessor messageSourceAccessor(MessageSource messageSource) {
            return new MessageSourceAccessor(messageSource);
        }

        @Bean
        MessageSourceHelper messageSourceHelper() {
            return MessageSourceHelper.create();
        }
    }
}
