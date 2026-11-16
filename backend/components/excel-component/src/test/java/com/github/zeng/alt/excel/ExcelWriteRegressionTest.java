package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集合与游标导出的 sheet 容量边界。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelWriteRegressionTest {

    @ParameterizedTest
    @CsvSource({"2,7,true", "2,7,false", "10,7,true", "10,7,false", "2,6,true", "2,0,false"})
    void splitsAtSheetCapacityRegardlessOfBatchSize(int batchSize, int rowCount, boolean iterator) throws Exception {
        final ExcelProperties properties = new ExcelProperties();
        properties.getWrite().setBatchSize(batchSize);
        properties.getWrite().setMaxRowsPerSheet(3);
        final ExcelTemplate template = new FesodExcelTemplate(new FesodExcelContext(
                properties,
                TestObjectProviders.empty(),
                TestObjectProviders.empty(),
                TestObjectProviders.empty(),
                TestObjectProviders.empty()
        ));
        final List<UserRow> rows = IntStream.range(0, rowCount).mapToObj(i -> new UserRow("u" + i, i + 1)).toList();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final ExcelWriteSpec<UserRow> spec = template.write(UserRow.class).to(output).i18nHead(false);

        assertThat((iterator ? spec.write(rows.iterator()) : spec.write(rows)).get()).isEqualTo((long) rowCount);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(Math.max(1, (rowCount + 2) / 3));
            assertThat(IntStream.range(0, workbook.getNumberOfSheets())
                    .map(i -> workbook.getSheetAt(i).getLastRowNum()).sum()).isEqualTo(rowCount);
            IntStream.range(0, workbook.getNumberOfSheets())
                    .forEach(i -> assertThat(workbook.getSheetAt(i).getLastRowNum()).isLessThanOrEqualTo(3));
        }
    }
}
