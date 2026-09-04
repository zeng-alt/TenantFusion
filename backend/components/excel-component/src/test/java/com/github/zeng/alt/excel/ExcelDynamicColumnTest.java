package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.dynamic.AbstractDynamicColumn;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 动态列读写测试。
 * <p>
 * 旧实现的动态列导出（{@code ExcelHelper#exportDynamicExcel}）算完表头就丢掉、
 * 调 {@code .sheet()} 后没有 {@code doWrite}，实际什么都不写；本类验证新实现
 * 两个方向都通。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelDynamicColumnTest {

    private final ExcelTemplate excelTemplate = new FesodExcelTemplate(new FesodExcelContext(
            new ExcelProperties(),
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.of(new DefaultFormattingConversionService())));

    @Test
    void 固定列绑字段其余列进动态单元格() {
        // 表头：姓名 + 三个按月份铺开的动态列
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.writeHead(List.of(List.of("userName"), List.of("一月"), List.of("二月")))
                .to(output)
                .i18nHead(false)
                .write(List.of(List.of("张三", "80", "90"), List.of("李四", "70", "60")))
                .get();

        ExcelReadResult<ScoreRow> result = excelTemplate.readDynamic(ScoreRow.class)
                .from(new ByteArrayInputStream(output.toByteArray()))
                .execute();

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.rows()).hasSize(2);

        ScoreRow first = result.rows().getFirst();
        assertThat(first.getUserName()).isEqualTo("张三");
        assertThat(first.getDynamicCells()).extracting(DynamicCell::getDisplayName)
                .containsExactly("一月", "二月");
        assertThat(first.getDynamicCells()).extracting(DynamicCell::getValue)
                .containsExactly("80", "90");
        assertThat(first.getDynamicCells()).extracting(DynamicCell::getColumnIndex)
                .containsExactly(1, 2);
    }

    @Test
    void 动态列可以按运行期表头导出() {
        ScoreRow row = new ScoreRow();
        row.setUserName("张三");
        row.addDynamicCell(DynamicCell.of(1, "一月", "一月", "80"));
        row.addDynamicCell(DynamicCell.of(2, "二月", "二月", "90"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Long written = excelTemplate.writeHead(row.dynamicHead())
                .to(output)
                .write(List.of(row.dynamicRow()))
                .get();

        assertThat(written).isEqualTo(1L);
        assertThat(output.size()).isPositive();
    }

    /**
     * 一个固定列 + 若干动态列的测试实体。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    public static class ScoreRow extends AbstractDynamicColumn<DynamicCell> {

        @ExcelProperty("userName")
        private String userName;
    }


    /** 编译期确认测试实体满足 readDynamic 的上界约束 */
    private static final Class<? extends DynamicColumn<DynamicCell>> BOUND_CHECK = ScoreRow.class;
}
