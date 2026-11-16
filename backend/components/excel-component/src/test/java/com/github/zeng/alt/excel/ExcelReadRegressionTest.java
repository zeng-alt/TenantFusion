package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import io.vavr.control.Try;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导入的错误边界、统计与整文件校验回归测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelReadRegressionTest {

    @TempDir
    Path directory;

    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ExcelProperties properties = new ExcelProperties();
    private final ExcelTemplate template = new FesodExcelTemplate(new FesodExcelContext(
            properties,
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.of(new ExcelRowValidator(validators.getValidator())),
            TestObjectProviders.of(new DefaultFormattingConversionService())
    ));

    @AfterEach
    void closeValidator() {
        validators.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"row", "batch", "tail", "predicate"})
    void consumerFailuresAreNeverSkipped(String mode) {
        final IllegalStateException failure = new IllegalStateException("database unavailable");
        final ExcelReadSpec<UserRow> spec = read(validRows()).onError(ExcelErrorPolicy.SKIP_ROW);
        final Try<Long> result = switch (mode) {
            case "row" -> spec.consume(row -> { throw failure; });
            case "batch" -> spec.batchSize(2).consumeBatch(rows -> { throw failure; });
            case "tail" -> spec.batchSize(10).consumeBatch(rows -> { throw failure; });
            default -> spec.consumeWhile(row -> { throw failure; });
        };

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).hasRootCause(failure);
    }

    @ParameterizedTest
    @ValueSource(strings = {"row", "batch", "predicate"})
    void collectAllRejectsBeforeAnyConsumerRuns(String mode) {
        final List<UserRow> accepted = new ArrayList<>();
        final ExcelReadSpec<UserRow> spec = read(List.of(
                new UserRow("A", 10), new UserRow("", 20), new UserRow("B", 30)
        )).onError(ExcelErrorPolicy.COLLECT_ALL);
        final Try<Long> result = switch (mode) {
            case "row" -> spec.consume(accepted::add);
            case "batch" -> spec.batchSize(1).consumeBatch(accepted::addAll);
            default -> spec.consumeWhile(accepted::add);
        };

        assertThat(result.isFailure()).isTrue();
        assertThat(accepted).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void collectAllReplaysValidStreamOrFileOnce(boolean stream) throws Exception {
        final byte[] bytes = workbook(validRows());
        final Path file = directory.resolve("users.xlsx");
        Files.write(file, bytes);
        final ExcelReadSpec<UserRow> spec = template.read(UserRow.class).onError(ExcelErrorPolicy.COLLECT_ALL);
        if (stream) {
            spec.from(new ByteArrayInputStream(bytes));
        } else {
            spec.from(file);
        }
        final List<UserRow> accepted = new ArrayList<>();

        assertThat(spec.consume(accepted::add).get()).isEqualTo(3L);
        assertThat(accepted).extracting(UserRow::getUserName).containsExactly("A", "B", "C");
    }

    @Test
    void conversionFailuresCountTowardSummaryAndRowLimit() throws Exception {
        final byte[] bytes = invalidNumbers();
        final ExcelReadResult<UserRow> result = template.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes)).execute();

        assertThat(result.summary().totalRows()).isEqualTo(3);
        assertThat(result.summary().errorRows()).isEqualTo(2);
        assertThat(result.summary().validRows()).isEqualTo(1);
        assertThat(result.rows()).hasSize(1);

        final ExcelReadResult<UserRow> limited = template.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes)).maxRows(1).execute();
        assertThat(limited.rows()).isEmpty();
        assertThat(limited.summary().truncated()).isTrue();
        assertThat(limited.summary().validRows()).isZero();
    }

    @Test
    void defaultAnnotationInheritsGlobalPolicyAndValidation() throws Exception {
        properties.getRead().setOnError(ExcelErrorPolicy.COLLECT_ALL);
        properties.getRead().setValidate(false);
        final ExcelImport annotation = getClass().getDeclaredMethod("importUsers", List.class)
                .getParameters()[0].getAnnotation(ExcelImport.class);
        final ExcelReadResult<?> result = new ExcelWebSpecFactory(template, properties)
                .readSpec(UserRow.class, annotation)
                .from(new ByteArrayInputStream(workbook(List.of(new UserRow("", 1))))).execute();

        assertThat(result.summary().policy()).isEqualTo(ExcelErrorPolicy.COLLECT_ALL);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void reportDescribesParsingWithoutClaimingDatabaseWrites() {
        final ExcelReadResult<UserRow> result = read(List.of(new UserRow("", 1)))
                .onError(ExcelErrorPolicy.COLLECT_ALL).execute();

        assertThat(result.toReport("users.xlsx").headline())
                .doesNotContain("未导入任何数据", "其余行已导入");
    }

    private void importUsers(@ExcelImport List<UserRow> users) {
    }

    private ExcelReadSpec<UserRow> read(List<UserRow> rows) {
        return template.read(UserRow.class).from(new ByteArrayInputStream(workbook(rows)));
    }

    private byte[] workbook(List<UserRow> rows) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.write(UserRow.class).to(output).i18nHead(false).write(rows).get();
        return output.toByteArray();
    }

    private byte[] invalidNumbers() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbook(validRows())))) {
            workbook.getSheetAt(0).getRow(1).getCell(1).setCellValue("invalid");
            workbook.getSheetAt(0).getRow(2).getCell(1).setCellValue("invalid");
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static List<UserRow> validRows() {
        return List.of(new UserRow("A", 10), new UserRow("B", 20), new UserRow("C", 30));
    }
}
