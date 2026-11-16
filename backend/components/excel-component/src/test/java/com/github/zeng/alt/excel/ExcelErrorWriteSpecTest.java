package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.write.ExcelErrorWriteCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelReadSummary;
import com.github.zeng.alt.excel.read.ExcelRowError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误工作簿保留原内容、样式和定位信息。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelErrorWriteSpecTest {

    @TempDir
    Path directory;

    private final ExcelProperties properties = new ExcelProperties();
    private final ExcelTemplate template = new FesodExcelTemplate(new FesodExcelContext(
            properties,
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.empty()
    ));

    @Test
    void marksCellsAndAppendsDetailsWithoutChangingOriginal() throws Exception {
        final Path source = source();
        final byte[] original = Files.readAllBytes(source);
        final Path target = directory.resolve("errors.xlsx");
        final ExcelReadResult<Object> result = errors();

        assertThat(template.writeErrors(result.toReport("source.xlsx")).from(source).to(target)
                .sheet("用户").execute().get()).isEqualTo(3L);

        assertThat(Files.readAllBytes(source)).isEqualTo(original);
        try (Workbook workbook = WorkbookFactory.create(target.toFile())) {
            final Cell error = workbook.getSheet("用户").getRow(1).getCell(0);
            assertThat(error.getStringCellValue()).isEqualTo("invalid");
            assertThat(error.getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(error.getCellComment().getString().getString()).contains("格式错误", "不能为空");
            assertThat(workbook.getSheet("用户").getRow(1).getCell(1).getCellFormula()).isEqualTo("1+1");
            assertThat(workbook.getSheet("错误明细")).isNotNull();
            assertThat(workbook.getSheet("错误明细_2").getLastRowNum()).isEqualTo(4);
            assertThat(workbook.getSheet("错误明细_2").getRow(3).getCell(5).getStringCellValue()).isEqualTo("invalid");
        }
    }

    @Test
    void refusesToOverwriteSourceFile() throws Exception {
        final Path source = source();
        final byte[] original = Files.readAllBytes(source);

        assertThat(template.writeErrors(errors().toReport("source.xlsx")).from(source).to(source)
                .execute().isFailure()).isTrue();
        assertThat(Files.readAllBytes(source)).isEqualTo(original);
    }

    @Test
    void honorsConfiguredDefaultsAndDoesNotCloseBorrowedOutput() throws Exception {
        properties.getErrorReport().setHighlight(false);
        properties.getErrorReport().setComments(false);
        properties.getErrorReport().setSheetName("问题");
        final AtomicBoolean closed = new AtomicBoolean();
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };

        template.writeErrors(errors().toReport("source.xlsx")).from(source()).to(output)
                .autoCloseStream(false).execute().get();

        assertThat(closed).isFalse();
        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getSheet("问题")).isNotNull();
            assertThat(workbook.getSheet("用户").getRow(1).getCell(0).getCellComment()).isNull();
            assertThat(workbook.getSheet("用户").getRow(1).getCell(0).getCellStyle().getFillPattern())
                    .isEqualTo(FillPatternType.NO_FILL);
        }
    }

    @Test
    void failedOutputIsClosedAndReported() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final OutputStream output = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("disk full");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertThat(template.writeErrors(errors().toReport("source.xlsx")).from(source()).to(output)
                .execute().isFailure()).isTrue();
        assertThat(closed).isTrue();
    }

    @Test
    void customizerOverridesConfigurationAndPerCallCanOverrideCustomizer() throws Exception {
        final Path source = source();
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ExcelAutoConfiguration.class))
                .withPropertyValues("alt.excel.error-report.sheet-name=配置名称")
                .withBean(ExcelErrorWriteCustomizer.class, () -> spec -> spec.summarySheet("贡献者名称"))
                .run(context -> {
                    final Path target = directory.resolve("customized.xlsx");
                    context.getBean(ExcelTemplate.class).writeErrors(errors().toReport("source.xlsx"))
                            .from(source).to(target).summarySheet("单次名称").execute().get();
                    try (Workbook workbook = WorkbookFactory.create(target.toFile())) {
                        assertThat(workbook.getSheet("单次名称")).isNotNull();
                        assertThat(workbook.getSheet("贡献者名称")).isNull();
                    }
                });
    }

    private ExcelReadResult<Object> errors() {
        return new ExcelReadResult<>(List.of(), List.of(
                ExcelRowError.ofCell(1, 0, "姓名", "格式错误"),
                ExcelRowError.ofCell(1, 0, "姓名", "不能为空"),
                ExcelRowError.ofRow(1, "跨字段约束失败")
        ), ExcelReadSummary.allValid(1));
    }

    private Path source() throws Exception {
        final Path source = directory.resolve("source.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            final Sheet sheet = workbook.createSheet("用户");
            sheet.createRow(0).createCell(0).setCellValue("姓名");
            final Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("invalid");
            row.createCell(1).setCellFormula("1+1");
            workbook.createSheet("错误明细");
            try (OutputStream output = Files.newOutputStream(source)) {
                workbook.write(output);
            }
        }
        return source;
    }
}
