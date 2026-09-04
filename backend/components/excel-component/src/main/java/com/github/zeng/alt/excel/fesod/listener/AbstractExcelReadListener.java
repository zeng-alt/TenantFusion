package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.fesod.ExcelReadAbort;
import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.read.ExcelReadSummary;
import com.github.zeng.alt.excel.read.ExcelRowError;
import com.github.zeng.alt.excel.read.ExcelViolation;
import com.github.zeng.alt.excel.support.ExcelFieldMeta;
import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import com.github.zeng.alt.excel.support.ExcelRowAccessor;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.exception.ExcelDataConvertException;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.apache.fesod.sheet.util.ConverterUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 读监听器基类：统一负责表头登记、逐行校验、失败明细收集与坏行策略。
 * <p>
 * 子类只需实现 {@link #toRow(Object, AnalysisContext)}，把 fesod 交出的原始行
 * 转成目标类型；成功行会先过 Bean Validation，再交给 {@link ExcelRowSink}。
 * <p>
 * 坏行策略（{@link ExcelReadOptions#policy()}）在三个地方生效：单元格转换失败走
 * {@link #onException}、行校验失败走 {@link #invoke}、错误条数触顶走
 * {@link #hasNext}。{@code FAIL_FAST} 会立刻抛 {@link ExcelReadAbort} 把 fesod 的
 * 解析循环拆掉，{@code COLLECT_ALL} 读完整个文件才在结局里标记整单驳回。
 *
 * @param <R> fesod 交出的原始行类型
 * @param <T> 目标行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public abstract class AbstractExcelReadListener<R, T> implements ReadListener<R> {

    private final ExcelRowSink<T> sink;
    private final ExcelReadOptions options;
    private final ExcelRowValidator validator;
    private final Class<T> rowType;

    private final List<ExcelRowError> errors = new ArrayList<>();
    private final Map<Integer, String> headers = new LinkedHashMap<>();
    private final Map<String, Integer> columnByField = new HashMap<>();
    private final Set<Integer> errorRows = new HashSet<>();
    private int totalRows;
    private boolean truncated;
    private boolean limitExceeded;

    /**
     * @param sink      成功行的去处
     * @param options   行为开关
     * @param validator 校验器，为 {@code null} 时不做 Bean Validation
     * @param rowType   行类型，用于把校验失败的字段定位到具体列；动态列场景传 {@code null}
     */
    protected AbstractExcelReadListener(ExcelRowSink<T> sink, ExcelReadOptions options,
                                        ExcelRowValidator validator, Class<T> rowType) {
        this.sink = sink;
        this.options = options;
        this.validator = validator;
        this.rowType = rowType;
    }

    /**
     * 把原始行转成目标类型。
     *
     * @param raw     fesod 交出的原始行
     * @param context 解析上下文
     * @return 成功为 {@code right(行)}，预期内的转换失败为 {@code left(原因)}
     */
    protected abstract Either<String, T> toRow(R raw, AnalysisContext context);

    /**
     * 表头登记完成的回调，子类需要按表头建列映射时覆写。
     *
     * @param headerByColumn 列下标 → 表头文本
     * @param context        解析上下文
     */
    protected void onHead(Map<Integer, String> headerByColumn, AnalysisContext context) {
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        headers.putAll(ConverterUtils.convertToStringMap(headMap, context));
        registerFieldColumns();
        onHead(Collections.unmodifiableMap(headers), context);
    }

    @Override
    public void invoke(R raw, AnalysisContext context) {
        if (exceedsRowLimit(context)) {
            return;
        }
        totalRows++;
        toRow(raw, context)
                .peekLeft(reason -> recordRowError(reason, context))
                .peek(row -> emit(row, context));
    }

    /**
     * 是否已经超过 {@code alt.excel.read.max-rows} 上限。
     * <p>
     * 超限只记一条错误就停，不逐行报——一份一百万行的文件报一百万条错等于没报。
     * 之后 {@link #hasNext} 会看到 {@link #limitExceeded} 从而结束解析。
     */
    private boolean exceedsRowLimit(AnalysisContext context) {
        if (!options.hasRowLimit() || totalRows < options.maxRows()) {
            return false;
        }
        if (!limitExceeded) {
            limitExceeded = true;
            addError(ExcelRowError.ofRow(rowIndexOf(context),
                    "数据行数超过上限 %d，已停止解析".formatted(options.maxRows())));
        }
        return true;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        // 中止信号原样上抛，否则会被自己的坏行策略又吞一次
        if (exception instanceof ExcelReadAbort || exception instanceof ExcelReadException) {
            throw exception;
        }
        if (exception instanceof ExcelDataConvertException convert) {
            recordCellError(convert);
        } else {
            recordRowError(exception.getMessage(), context);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        sink.complete();
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        if (sink.isCancelled() || limitExceeded) {
            return false;
        }
        if (errors.size() >= options.maxErrors()) {
            // 错误已经多到没有继续读的意义，标记截断让前端能提示「还有更多」
            truncated = true;
            return false;
        }
        return true;
    }

    /**
     * 失败明细。
     *
     * @return 不可变视图
     */
    public List<ExcelRowError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * 本次读取的统计与结局。
     *
     * @return 统计
     */
    public ExcelReadSummary getSummary() {
        boolean aborted = !errors.isEmpty() && options.policy().rejectsWholeFile();
        return new ExcelReadSummary(
                options.policy(), totalRows, errorRows.size(), truncated || limitExceeded, aborted);
    }

    /**
     * 成功行数。
     *
     * @return 行数
     */
    public long getRowCount() {
        return sink.count();
    }

    /**
     * 列下标 → 表头文本。
     *
     * @return 不可变视图
     */
    protected Map<Integer, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    // ==================== 校验与记账 ====================

    private void emit(T row, AnalysisContext context) {
        List<ExcelViolation> violations = validate(row);
        if (violations.isEmpty()) {
            sink.accept(row);
            return;
        }
        int rowIndex = rowIndexOf(context);
        for (ExcelViolation violation : violations) {
            addError(ExcelRowError.ofViolation(
                    rowIndex, columnOf(violation.field()), headerOf(violation.field()), violation));
        }
        abortIfNeeded();
    }

    private List<ExcelViolation> validate(T row) {
        if (!options.validate() || validator == null) {
            return List.of();
        }
        return validator.validate(row, options.validationGroups());
    }

    private void recordRowError(String reason, AnalysisContext context) {
        addError(ExcelRowError.ofRow(rowIndexOf(context), reason));
        abortIfNeeded();
    }

    private void recordCellError(ExcelDataConvertException exception) {
        int rowIndex = exception.getRowIndex() == null ? -1 : exception.getRowIndex();
        int columnIndex = exception.getColumnIndex() == null ? -1 : exception.getColumnIndex();
        addError(ExcelRowError.ofCell(rowIndex, columnIndex, headers.get(columnIndex), "单元格格式无法解析"));
        abortIfNeeded();
    }

    private void addError(ExcelRowError error) {
        errors.add(error);
        errorRows.add(error.rowNumber());
    }

    /**
     * {@code FAIL_FAST} 下立刻拆掉解析循环；{@code COLLECT_ALL} 与 {@code SKIP_ROW}
     * 都继续读，区别只在最后是否整单驳回。
     */
    private void abortIfNeeded() {
        if (options.policy().abortsOnFirstError()) {
            throw new ExcelReadAbort(errors.getLast().describe());
        }
    }

    // ==================== 字段 → 列定位 ====================

    /**
     * 建立「字段名 → 列下标」，让校验失败能精确定位到单元格而不只是行。
     * <p>
     * 靠本组件自己的 {@link ExcelRowAccessor} 元数据（字段 → 表头声明）与实际读到的
     * 表头文本对撞得出，因此两种绑定方式下都可用；表头声明是 {@code {i18n.key}} 时
     * 先解析成当前语言的文本再匹配。匹配不上就退回行级定位（列号 -1），不报错。
     */
    private void registerFieldColumns() {
        if (rowType == null) {
            return;
        }
        Map<String, Integer> columnByHeader = new HashMap<>();
        headers.forEach((column, header) -> {
            if (header != null) {
                columnByHeader.put(header.trim(), column);
            }
        });
        for (ExcelFieldMeta field : ExcelRowAccessor.of(rowType).getFields()) {
            Integer column = columnByHeader.get(ExcelMessageHelper.resolve(field.headCode()).trim());
            if (column == null) {
                column = columnByHeader.get(field.headCode().trim());
            }
            if (column != null) {
                columnByField.put(field.fieldName(), column);
            }
        }
    }

    private int columnOf(String field) {
        Integer column = columnByField.get(field);
        return column == null ? ExcelRowError.NO_COLUMN : column;
    }

    private String headerOf(String field) {
        Integer column = columnByField.get(field);
        return column == null ? "" : headers.getOrDefault(column, "");
    }

    private static int rowIndexOf(AnalysisContext context) {
        if (context == null || context.readRowHolder() == null) {
            return -1;
        }
        Integer rowIndex = context.readRowHolder().getRowIndex();
        return rowIndex == null ? -1 : rowIndex;
    }
}
