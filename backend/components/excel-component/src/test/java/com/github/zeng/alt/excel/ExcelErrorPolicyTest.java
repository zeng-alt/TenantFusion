package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.exception.ExcelValidationException;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelErrorReport;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelRowError;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三种坏行策略与给前端的错误报告。
 * <p>
 * 之前只有一个 {@code skipInvalidRows} 布尔，表达不了「校验完整个文件再整单驳回」
 * 这个最常用的导入语义——用户改一行传一次是最糟的体验。这里钉住三种策略的
 * 行为差异，以及报告里前端要用的每一项。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelErrorPolicyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ExcelAutoConfiguration.class,
                    ValidationAutoConfiguration.class));

    /** 好、坏、好、坏、好——用来区分「读到哪一行才停」 */
    private static final List<UserRow> MIXED = List.of(
            new UserRow("张三", 18),
            new UserRow("", 20),
            new UserRow("王五", 22),
            new UserRow("赵六", 0),
            new UserRow("孙七", 24));

    // ==================== SKIP_ROW：部分成功 ====================

    @Test
    void skipRowKeepsGoodRowsAndReportsBadOnes() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelReadResult<UserRow> result = read(excelTemplate, ExcelErrorPolicy.SKIP_ROW);

            assertThat(result.rows()).extracting(UserRow::getUserName)
                    .containsExactly("张三", "王五", "孙七");
            assertThat(result.errors()).hasSize(2);
            assertThat(result.isAborted()).isFalse();
            assertThat(result.summary().totalRows()).isEqualTo(5);
            assertThat(result.summary().errorRows()).isEqualTo(2);
            assertThat(result.summary().validRows()).isEqualTo(3);
        });
    }

    // ==================== FAIL_FAST：马上中断 ====================

    @Test
    void failFastStopsAtFirstBadRow() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelReadResult<UserRow> result = read(excelTemplate, ExcelErrorPolicy.FAIL_FAST);

            // 只读到第 2 行就停了，后面的 3 行根本没解析
            assertThat(result.summary().totalRows()).isEqualTo(2);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().field()).isEqualTo("userName");
            assertThat(result.isAborted()).isTrue();
        });
    }

    @Test
    void failFastDoesNotThrowFromExecute() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            // execute() 永远不抛坏行异常，结局在 isAborted() 里
            assertThat(read(excelTemplate, ExcelErrorPolicy.FAIL_FAST).isAborted()).isTrue();
        });
    }

    @Test
    void failFastMakesConsumeFailWithReport() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            List<String> consumed = new ArrayList<>();

            Try<Long> count = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file(excelTemplate)))
                    .onError(ExcelErrorPolicy.FAIL_FAST)
                    .consume(row -> consumed.add(row.getUserName()));

            // 逐行消费没有地方承载明细，所以整单驳回表达成 Try.failure + 异常带报告
            assertThat(count.isFailure()).isTrue();
            assertThat(count.getCause()).isInstanceOf(ExcelValidationException.class);
            ExcelErrorReport report = ((ExcelValidationException) count.getCause()).getReport();
            assertThat(report.errors()).hasSize(1);
            assertThat(report.summary().aborted()).isTrue();
            // 中断前已经下发的好行
            assertThat(consumed).containsExactly("张三");
        });
    }

    // ==================== COLLECT_ALL：校验完整个文件再中断 ====================

    @Test
    void collectAllReadsWholeFileThenRejects() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelReadResult<UserRow> result = read(excelTemplate, ExcelErrorPolicy.COLLECT_ALL);

            // 整个文件都读完了，所以两条错误都在
            assertThat(result.summary().totalRows()).isEqualTo(5);
            assertThat(result.errors()).hasSize(2);
            assertThat(result.errors()).extracting(ExcelRowError::rowNumber).containsExactly(3, 5);
            // 但整单驳回，好行不该入库
            assertThat(result.isAborted()).isTrue();
        });
    }

    @Test
    void collectAllDiffersFromSkipRowOnlyInTheVerdict() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelReadResult<UserRow> skip = read(excelTemplate, ExcelErrorPolicy.SKIP_ROW);
            ExcelReadResult<UserRow> collect = read(excelTemplate, ExcelErrorPolicy.COLLECT_ALL);

            // 读到的东西一模一样
            assertThat(collect.rows()).hasSameSizeAs(skip.rows());
            assertThat(collect.errors()).hasSameSizeAs(skip.errors());
            // 区别只在结局
            assertThat(skip.isAborted()).isFalse();
            assertThat(collect.isAborted()).isTrue();
        });
    }

    // ==================== 给前端的报告 ====================

    @Test
    void reportGroupsErrorsByRowForCellHighlighting() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(new UserRow("", 0), new UserRow("王五", 22)));

            ExcelErrorReport report = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .onError(ExcelErrorPolicy.COLLECT_ALL)
                    .execute()
                    .toReport("users.xlsx");

            assertThat(report.fileName()).isEqualTo("users.xlsx");
            // 平铺明细供表格渲染
            assertThat(report.errors()).hasSize(2);
            // 按行分组供「某一行展开看全部问题」和单元格红框
            assertThat(report.rows()).containsOnlyKeys(2);
            assertThat(report.rows().get(2)).hasSize(2);
            // 按约束码计数供分类提示
            assertThat(report.codes()).containsEntry("NotBlank", 1).containsEntry("Min", 1);
        });
    }

    @Test
    void reportHeadlineIsReadyToShow() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelErrorReport report = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file(excelTemplate)))
                    .onError(ExcelErrorPolicy.COLLECT_ALL)
                    .execute()
                    .toReport("users.xlsx");

            assertThat(report.headline())
                    .contains("users.xlsx")
                    .contains("共 5 行")
                    .contains("2 行有问题")
                    .contains("未导入任何数据");
        });
    }

    @Test
    void reportErrorsAreSortedByRowThenColumn() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(
                    new UserRow("王五", 0), new UserRow("", 0)));

            ExcelErrorReport report = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .onError(ExcelErrorPolicy.COLLECT_ALL)
                    .execute()
                    .toReport(null);

            // 顺序稳定，前端表格不会每次刷新都跳
            assertThat(report.errors()).extracting(ExcelRowError::rowNumber).containsExactly(2, 3, 3);
            assertThat(report.errors()).extracting(ExcelRowError::columnNumber).containsExactly(2, 1, 2);
        });
    }

    @Test
    void maxErrorsMarksReportAsTruncated() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            List<UserRow> manyBad = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                manyBad.add(new UserRow("", 18));
            }
            byte[] file = write(excelTemplate, manyBad);

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .maxErrors(5)
                    .onError(ExcelErrorPolicy.COLLECT_ALL)
                    .execute();

            assertThat(result.errors()).hasSizeLessThanOrEqualTo(6);
            assertThat(result.summary().truncated()).isTrue();
            assertThat(result.toReport("big.xlsx").headline()).contains("错误过多已截断");
        });
    }

    // ==================== 辅助 ====================

    private static ExcelReadResult<UserRow> read(ExcelTemplate excelTemplate, ExcelErrorPolicy policy) {
        return excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(file(excelTemplate)))
                .onError(policy)
                .execute();
    }

    private static byte[] file(ExcelTemplate excelTemplate) {
        return write(excelTemplate, MIXED);
    }

    private static byte[] write(ExcelTemplate excelTemplate, List<UserRow> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class).to(output).i18nHead(false).write(rows).get();
        return output.toByteArray();
    }
}
