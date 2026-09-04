package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebMvcAutoConfiguration;
import com.github.zeng.alt.excel.exception.ExcelException;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.web.ExcelReactiveSupport;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 没有 RxJava 时组件依然可用。
 * <p>
 * RxJava 是本模块的可选依赖，所以核心 SPI 的签名里不能出现响应式类型——否则
 * 反射枚举实现类的方法就会抛 {@code NoClassDefFoundError}。这里用
 * {@link FilteredClassLoader} 把 RxJava 从 classpath 上摘掉，验证：
 * <ul>
 *   <li>上下文照常启动，{@code ExcelTemplate} 与 MVC 集成都装配得出来</li>
 *   <li>读写、逐行消费全部正常</li>
 *   <li>只有真的用响应式形状时才报错，且是可操作的提示而不是 {@code NoClassDefFoundError}</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelWithoutRxJavaTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Flowable.class))
            .withConfiguration(AutoConfigurations.of(
                    ExcelAutoConfiguration.class,
                    ExcelWebAutoConfiguration.class,
                    ExcelWebMvcAutoConfiguration.class,
                    ValidationAutoConfiguration.class,
                    WebMvcAutoConfiguration.class));

    @Test
    void contextStartsWithoutRxJava() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ExcelTemplate.class);
            assertThat(context).hasSingleBean(ExcelReactiveSupport.class);
        });
    }

    @Test
    void reactiveSupportFallsBackToNoOp() {
        runner.run(context -> {
            ExcelReactiveSupport support = context.getBean(ExcelReactiveSupport.class);

            assertThat(support.getClass().getSimpleName()).isEqualTo("NoOpExcelReactiveSupport");
            assertThat(support.supports(List.class)).isFalse();
        });
    }

    @Test
    void readAndWriteStillWorkWithoutRxJava() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            excelTemplate.write(UserRow.class)
                    .to(output)
                    .i18nHead(false)
                    .write(List.of(new UserRow("张三", 18), new UserRow("李四", 30)))
                    .get();

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(output.toByteArray()))
                    .execute();

            assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三", "李四");
        });
    }

    @Test
    void consumeWhileCoversStreamingWithoutRxJava() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            excelTemplate.write(UserRow.class)
                    .to(output)
                    .i18nHead(false)
                    .write(List.of(new UserRow("A", 1), new UserRow("B", 2), new UserRow("C", 3)))
                    .get();

            List<String> collected = new ArrayList<>();
            Try<Long> count = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(output.toByteArray()))
                    .consumeWhile(row -> {
                        collected.add(row.getUserName());
                        return collected.size() < 2;
                    });

            // 提前收工：第三行不再解析
            assertThat(count.get()).isEqualTo(2L);
            assertThat(collected).containsExactly("A", "B");
        });
    }

    @Test
    void writeFromIteratorStillWorksWithoutRxJava() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Long written = excelTemplate.write(UserRow.class)
                    .to(output)
                    .i18nHead(false)
                    .write(List.of(new UserRow("A", 1), new UserRow("B", 2)).iterator())
                    .get();

            assertThat(written).isEqualTo(2L);
        });
    }

    @Test
    void reactiveShapeReportsActionableErrorWithoutRxJava() {
        runner.run(context -> {
            ExcelReactiveSupport support = context.getBean(ExcelReactiveSupport.class);

            assertThatThrownBy(support::emptyStream)
                    .isInstanceOf(ExcelException.class)
                    .hasMessageContaining("io.reactivex.rxjava3:rxjava");
            assertThat(support.iterator(null).isFailure()).isTrue();
        });
    }
}
