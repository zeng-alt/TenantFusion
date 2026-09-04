package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.rx.RxExcel;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Try;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExcelTemplate} 的读写往返测试。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelTemplateTest {

    private final ExcelProperties properties = new ExcelProperties();
    private final ExcelTemplate excelTemplate = newTemplate();

    private ExcelTemplate newTemplate() {
        ExcelRowValidator validator = new ExcelRowValidator(
                Validation.buildDefaultValidatorFactory().getValidator());
        return new FesodExcelTemplate(new FesodExcelContext(
                properties,
                TestObjectProviders.empty(),
                TestObjectProviders.empty(),
                TestObjectProviders.of(validator),
                TestObjectProviders.of(new DefaultFormattingConversionService())));
    }

    @Test
    void roundTripsEntityRows() {
        byte[] bytes = writeUsers(new UserRow("张三", 18), new UserRow("李四", 30));

        ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute();

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三", "李四");
        assertThat(result.rows()).extracting(UserRow::getAge).containsExactly(18, 30);
    }

    @Test
    void collectsInvalidRowsAsErrorsInsteadOfThrowing() {
        byte[] bytes = writeUsers(new UserRow("张三", 18), new UserRow("", 30));

        ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute();

        assertThat(result.rows()).hasSize(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().message()).contains("姓名不能为空");
        // 第 1 行是表头，坏行是文件里的第 3 行
        assertThat(result.errors().getFirst().rowNumber()).isEqualTo(3);
        assertThat(result.toEither().isLeft()).isTrue();
    }

    @Test
    void stopsAtFirstInvalidRowWhenSkipDisabled() {
        byte[] bytes = writeUsers(new UserRow("", 18), new UserRow("李四", 30));

        ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .skipInvalidRows(false)
                .execute();

        // 第一行就坏，后面的「李四」不再被读
        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.toEither().isLeft()).isTrue();
    }

    @Test
    void failsConsumeWhenSkipDisabled() {
        byte[] bytes = writeUsers(new UserRow("", 18));

        Try<Long> count = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .skipInvalidRows(false)
                .consume(row -> {
                });

        assertThat(count.isFailure()).isTrue();
    }

    @Test
    void errorsFlowableWhenSkipDisabled() {
        byte[] bytes = writeUsers(new UserRow("", 18));

        assertThatThrownBy(() -> RxExcel.stream(excelTemplate.read(UserRow.class)
                        .from(new ByteArrayInputStream(bytes))
                        .skipInvalidRows(false))
                .toList()
                .blockingGet())
                .hasRootCauseInstanceOf(ExcelReadException.class);
    }

    @Test
    void acceptsInvalidRowsWhenValidationDisabled() {
        byte[] bytes = writeUsers(new UserRow("", 18));

        ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .validate(false)
                .execute();

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void emitsRowsLazilyAndHonoursCancellation() {
        byte[] bytes = writeUsers(new UserRow("张三", 1), new UserRow("李四", 2), new UserRow("王五", 3));

        List<String> names = RxExcel.stream(excelTemplate.read(UserRow.class)
                        .from(new ByteArrayInputStream(bytes)))
                .map(UserRow::getUserName)
                .take(2)
                .toList()
                .blockingGet();

        assertThat(names).containsExactly("张三", "李四");
    }

    @Test
    void returnsRowCountFromConsume() {
        byte[] bytes = writeUsers(new UserRow("张三", 1), new UserRow("李四", 2));
        List<String> collected = new ArrayList<>();

        Try<Long> count = excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .consume(row -> collected.add(row.getUserName()));

        assertThat(count.get()).isEqualTo(2L);
        assertThat(collected).containsExactly("张三", "李四");
    }

    @Test
    void exportsWithRuntimeHeadWithoutEntity() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Try<Long> written = excelTemplate.writeHead(List.of(List.of("一月"), List.of("二月")))
                .to(output)
                .write(List.of(List.of("100", "200")));

        assertThat(written.get()).isEqualTo(1L);
        assertThat(output.size()).isPositive();
    }

    @Test
    void reportsMissingSourceClearly() {
        assertThatThrownBy(() -> excelTemplate.read(UserRow.class).execute())
                .isInstanceOf(ExcelReadException.class)
                .hasMessageContaining("未指定数据源");
    }

    private byte[] writeUsers(UserRow... rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // 显式关掉表头国际化：本类验证的是 fesod 原生字面量匹配，
        // 开着的话表头会被替换成当前语言文本，读回时就对不上 @ExcelProperty 的 key
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .write(Stream.of(rows).toList())
                .get();
        return output.toByteArray();
    }

}
