package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.support.ExcelFileNameHelper;
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
 * 全局属性配置：默认值、配置文件覆盖、链上覆盖三层的优先级。
 * <p>
 * 每个配置项都该能在三个层次上表达——配置文件给全局默认、链式方法按次覆盖、
 * 注解按接口覆盖。这里钉住新增的几项，以及「链上传的值确实压得住配置文件」。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ExcelAutoConfiguration.class,
                    ValidationAutoConfiguration.class));

    // ==================== 默认值 ====================

    @Test
    void defaultsAreSpelledOutInProperties() {
        runner.run(context -> {
            ExcelProperties properties = context.getBean(ExcelProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getRead().getBatchSize()).isEqualTo(500);
            assertThat(properties.getRead().getMaxRows()).isEqualTo(-1);
            assertThat(properties.getRead().isValidate()).isTrue();
            assertThat(properties.getRead().getOnError()).isEqualTo(ExcelErrorPolicy.SKIP_ROW);
            assertThat(properties.getRead().getMaxErrors()).isEqualTo(1000);
            assertThat(properties.getWrite().getBatchSize()).isEqualTo(2000);
            assertThat(properties.getWrite().getMaxRowsPerSheet()).isEqualTo(1_000_000);
            assertThat(properties.getWrite().getFileNameTimestampPattern()).isEqualTo("yyyyMMddHHmmss");
        });
    }

    @Test
    void everyNewPropertyIsBindableFromConfig() {
        runner.withPropertyValues(
                        "alt.excel.read.batch-size=250",
                        "alt.excel.read.max-rows=10",
                        "alt.excel.read.validate=false",
                        "alt.excel.read.on-error=collect_all",
                        "alt.excel.write.max-rows-per-sheet=5",
                        "alt.excel.write.file-name-timestamp-pattern=yyyyMMdd")
                .run(context -> {
                    ExcelProperties properties = context.getBean(ExcelProperties.class);

                    assertThat(properties.getRead().getBatchSize()).isEqualTo(250);
                    assertThat(properties.getRead().getMaxRows()).isEqualTo(10);
                    assertThat(properties.getRead().isValidate()).isFalse();
                    assertThat(properties.getRead().getOnError()).isEqualTo(ExcelErrorPolicy.COLLECT_ALL);
                    assertThat(properties.getWrite().getMaxRowsPerSheet()).isEqualTo(5);
                    assertThat(properties.getWrite().getFileNameTimestampPattern()).isEqualTo("yyyyMMdd");
                });
    }

    @Test
    void rootSwitchTurnsTheWholeComponentOff() {
        runner.withPropertyValues("alt.excel.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ExcelTemplate.class);
                    assertThat(context).doesNotHaveBean(ExcelProperties.class);
                });
    }

    // ==================== 批量导入 ====================

    @Test
    void batchSizeFromConfigDrivesConsumeBatch() {
        runner.withPropertyValues("alt.excel.read.batch-size=2")
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    List<Integer> batchSizes = new ArrayList<>();

                    Try<Long> count = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file(excelTemplate, 5)))
                            .consumeBatch(batch -> batchSizes.add(batch.size()));

                    // 5 行、每批 2 条 => 2 + 2 + 1，末批不足也要下发
                    assertThat(count.get()).isEqualTo(5L);
                    assertThat(batchSizes).containsExactly(2, 2, 1);
                });
    }

    @Test
    void chainedBatchSizeOverridesConfig() {
        runner.withPropertyValues("alt.excel.read.batch-size=2")
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    List<Integer> batchSizes = new ArrayList<>();

                    excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file(excelTemplate, 5)))
                            .batchSize(5)
                            .consumeBatch(batch -> batchSizes.add(batch.size()))
                            .get();

                    assertThat(batchSizes).containsExactly(5);
                });
    }

    @Test
    void emptyFileNeverCallsBatchConsumer() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            List<Integer> batchSizes = new ArrayList<>();

            Try<Long> count = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file(excelTemplate, 0)))
                    .consumeBatch(batch -> batchSizes.add(batch.size()));

            assertThat(count.get()).isZero();
            assertThat(batchSizes).isEmpty();
        });
    }

    @Test
    void batchesAreIndependentSnapshots() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            List<List<UserRow>> kept = new ArrayList<>();

            excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file(excelTemplate, 4)))
                    .batchSize(2)
                    .consumeBatch(kept::add)
                    .get();

            // 下游很可能异步持有批次，所以缓冲不能复用——前一批不该被后一批覆盖
            assertThat(kept).hasSize(2);
            assertThat(kept.getFirst()).hasSize(2);
            assertThat(kept.getFirst().getFirst().getUserName()).isEqualTo("u0");
            assertThat(kept.getLast().getFirst().getUserName()).isEqualTo("u2");
        });
    }

    // ==================== 行数上限 ====================

    @Test
    void maxRowsTruncatesAndRecordsOneError() {
        runner.withPropertyValues("alt.excel.read.max-rows=3")
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

                    ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file(excelTemplate, 10)))
                            .execute();

                    assertThat(result.rows()).hasSize(3);
                    // 超限只记一条错误，不逐行报——一百万行报一百万条等于没报
                    assertThat(result.errors()).hasSize(1);
                    assertThat(result.errors().getFirst().message()).contains("超过上限 3");
                    assertThat(result.summary().truncated()).isTrue();
                });
    }

    @Test
    void maxRowsAbortsUnderCollectAll() {
        runner.withPropertyValues("alt.excel.read.max-rows=3")
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

                    ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file(excelTemplate, 10)))
                            .onError(ExcelErrorPolicy.COLLECT_ALL)
                            .execute();

                    assertThat(result.isAborted()).isTrue();
                });
    }

    @Test
    void unlimitedByDefault() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file(excelTemplate, 50)))
                    .execute();

            assertThat(result.rows()).hasSize(50);
            assertThat(result.errors()).isEmpty();
        });
    }

    // ==================== 导出分 sheet ====================

    @Test
    void maxRowsPerSheetRollsOverToNewSheet() {
        runner.withPropertyValues(
                        "alt.excel.write.batch-size=2",
                        "alt.excel.write.max-rows-per-sheet=2")
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();

                    Long written = excelTemplate.write(UserRow.class)
                            .to(output)
                            .sheet("用户")
                            .i18nHead(false)
                            .write(rows(6).iterator())
                            .get();

                    assertThat(written).isEqualTo(6L);
                    // 6 行、每 sheet 2 行 => 3 个 sheet，第一个沿用指定名字
                    ExcelReadResult<UserRow> first = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(output.toByteArray()))
                            .sheet("用户")
                            .execute();
                    ExcelReadResult<UserRow> second = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(output.toByteArray()))
                            .sheet("用户_2")
                            .execute();

                    assertThat(first.rows()).hasSize(2);
                    assertThat(second.rows()).hasSize(2);
                });
    }

    // ==================== 文件名时间戳 ====================

    @Test
    void fileNameTimestampPatternIsApplied() {
        String name = ExcelFileNameHelper.build("用户清单", true, "yyyyMMdd");

        assertThat(name).matches("用户清单_\\d{8}\\.xlsx");
    }

    @Test
    void illegalTimestampPatternFallsBackInsteadOfFailing() {
        // "#" 是 DateTimeFormatter 的保留字符，ofPattern 会直接抛
        String name = ExcelFileNameHelper.build("用户清单", true, "#");

        // 配置笔误不该把导出整个搞挂
        assertThat(name).matches("用户清单_\\d{14}\\.xlsx");
    }

    @Test
    void blankTimestampPatternUsesDefault() {
        assertThat(ExcelFileNameHelper.build("用户清单", true, "  "))
                .matches("用户清单_\\d{14}\\.xlsx");
    }

    // ==================== 辅助 ====================

    private static List<UserRow> rows(int size) {
        List<UserRow> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rows.add(new UserRow("u" + i, i + 1));
        }
        return rows;
    }

    private static byte[] file(ExcelTemplate excelTemplate, int size) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class).to(output).i18nHead(false).write(rows(size)).get();
        return output.toByteArray();
    }
}
