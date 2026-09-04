package com.github.zeng.alt.excel.read;

import io.vavr.control.Either;

import java.util.Collections;
import java.util.List;

/**
 * 一次读取的完整结果：成功行与失败明细并存。
 * <p>
 * 批量导入的常态是「部分成功」，所以读取的终结方法既不抛异常也不返回 {@code null}，
 * 而是把两半都交出来，由调用方决定是整单驳回还是先入库好行。
 * <p>
 * 用 {@code java.util.List} 而非 Vavr 集合：本类会直接出现在 controller 的返回值上，
 * 需要经过 Jackson，而本模块没有注册 {@code vavr-jackson}。
 *
 * @param <T>     行类型
 * @param rows    成功解析并通过校验的行，永不为 {@code null}
 * @param errors  失败明细，永不为 {@code null}
 * @param summary 统计与结局（用了哪种坏行策略、是否整单驳回）
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelReadResult<T>(List<T> rows, List<ExcelRowError> errors, ExcelReadSummary summary) {

    public ExcelReadResult {
        rows = rows == null ? List.of() : List.copyOf(rows);
        errors = errors == null ? List.of() : List.copyOf(errors);
        summary = summary == null ? ExcelReadSummary.empty() : summary;
    }

    /**
     * 兼容两参构造：统计按「跳过坏行」策略推算。
     *
     * @param rows   成功行
     * @param errors 失败明细
     */
    public ExcelReadResult(List<T> rows, List<ExcelRowError> errors) {
        this(rows, errors, inferSummary(rows, errors));
    }

    private static ExcelReadSummary inferSummary(List<?> rows, List<ExcelRowError> errors) {
        int errorRows = errors == null ? 0
                : (int) errors.stream().map(ExcelRowError::rowNumber).distinct().count();
        int total = (rows == null ? 0 : rows.size()) + errorRows;
        return new ExcelReadSummary(ExcelErrorPolicy.SKIP_ROW, total, errorRows, false, false);
    }

    /**
     * 空结果。
     *
     * @param <T> 行类型
     * @return 无数据无错误的结果
     */
    public static <T> ExcelReadResult<T> empty() {
        return new ExcelReadResult<>(Collections.emptyList(), Collections.emptyList(), ExcelReadSummary.empty());
    }

    /**
     * 全部成功的结果。
     *
     * @param rows 成功行
     * @param <T>  行类型
     * @return 结果
     */
    public static <T> ExcelReadResult<T> of(List<T> rows) {
        return new ExcelReadResult<>(rows, Collections.emptyList(),
                ExcelReadSummary.allValid(rows == null ? 0 : rows.size()));
    }

    /**
     * 是否存在失败行。
     *
     * @return true 表示至少有一行失败
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 是否一行都没读到。
     *
     * @return true 表示无成功行
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * 是否整单驳回：坏行策略是 {@code FAIL_FAST} / {@code COLLECT_ALL} 且确实有错误。
     * <p>
     * 为 {@code true} 时 {@link #rows()} 里的行<b>不应该</b>被入库——它们只是中断前
     * 已经读到的部分。
     *
     * @return true 表示整单驳回
     */
    public boolean isAborted() {
        return summary.aborted();
    }

    /**
     * 折叠成「要么全错、要么全对」的形态，适合直接接到 service 的 {@code Either} 链上。
     *
     * @return 有失败行时为 {@code left(错误明细)}，否则 {@code right(成功行)}
     */
    public Either<List<ExcelRowError>, List<T>> toEither() {
        return hasErrors() ? Either.left(errors) : Either.right(rows);
    }

    /**
     * 折叠成「要么全错、要么全对」，左值是可直接给前端的报告。
     *
     * @param fileName 文件名，可为 {@code null}
     * @return 有失败行时为 {@code left(报告)}，否则 {@code right(成功行)}
     */
    public Either<ExcelErrorReport, List<T>> toEither(String fileName) {
        return hasErrors() ? Either.left(toReport(fileName)) : Either.right(rows);
    }

    /**
     * 生成给前端的错误报告。
     *
     * @param fileName 文件名，可为 {@code null}
     * @return 报告
     */
    public ExcelErrorReport toReport(String fileName) {
        return ExcelErrorReport.of(fileName, this);
    }

    /**
     * 失败明细拼成一段可直接回显的文本。
     *
     * @return 多行文本，无失败时为空串
     */
    public String describeErrors() {
        return errors.stream().map(ExcelRowError::describe).reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }
}
