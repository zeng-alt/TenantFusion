package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.read.ExcelRowError;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.exception.ExcelDataConvertException;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.apache.fesod.sheet.util.ConverterUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读监听器基类：统一负责表头登记、逐行校验、失败明细收集与坏行策略。
 * <p>
 * 子类只需实现 {@link #toRow(Object, AnalysisContext)}，把 fesod 交出的原始行
 * 转成目标类型；成功行会先过 Bean Validation，再交给 {@link ExcelRowSink}。
 * <p>
 * 坏行策略（{@link ExcelReadOptions#skipInvalidRows()}）在两个地方生效：
 * 单元格转换失败走 {@link #onException}，行校验失败走 {@link #invoke}，
 * 两者都遵循「跳过并记账」或「立即中止」这一个开关。
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
    private final List<ExcelRowError> errors = new ArrayList<>();
    private final Map<Integer, String> headers = new LinkedHashMap<>();

    /**
     * @param sink      成功行的去处
     * @param options   行为开关
     * @param validator 校验器，为 {@code null} 时不做 Bean Validation
     */
    protected AbstractExcelReadListener(ExcelRowSink<T> sink, ExcelReadOptions options, ExcelRowValidator validator) {
        this.sink = sink;
        this.options = options;
        this.validator = validator;
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
        onHead(Collections.unmodifiableMap(headers), context);
    }

    @Override
    public void invoke(R raw, AnalysisContext context) {
        toRow(raw, context)
                .peekLeft(reason -> recordRowError(reason, context))
                .peek(row -> emit(row, context));
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        // 中止信号原样上抛，否则会被自己的坏行策略又吞一次
        if (exception instanceof ExcelReadException) {
            throw exception;
        }
        if (exception instanceof ExcelDataConvertException convert) {
            recordCellError(convert);
        } else {
            recordRowError(exception.getMessage(), context);
        }
        if (!options.skipInvalidRows()) {
            throw exception;
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        sink.complete();
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return !sink.isCancelled() && errors.size() < options.maxErrors();
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

    private void emit(T row, AnalysisContext context) {
        Option<String> invalid = validate(row);
        if (invalid.isDefined()) {
            recordRowError(invalid.get(), context);
            return;
        }
        sink.accept(row);
    }

    private Option<String> validate(T row) {
        if (!options.validate() || validator == null) {
            return Option.none();
        }
        return validator.validate(row);
    }

    private void recordRowError(String reason, AnalysisContext context) {
        errors.add(ExcelRowError.ofRow(rowIndexOf(context), reason == null ? "解析失败" : reason));
        abortIfNeeded();
    }

    private void recordCellError(ExcelDataConvertException exception) {
        int rowIndex = exception.getRowIndex() == null ? -1 : exception.getRowIndex();
        int columnIndex = exception.getColumnIndex() == null ? -1 : exception.getColumnIndex();
        errors.add(ExcelRowError.ofCell(
                rowIndex, columnIndex, headers.get(columnIndex), "单元格格式无法解析"));
    }

    private void abortIfNeeded() {
        if (!options.skipInvalidRows()) {
            throw new ExcelReadException(errors.getLast().describe());
        }
    }

    private static int rowIndexOf(AnalysisContext context) {
        if (context == null || context.readRowHolder() == null) {
            return -1;
        }
        Integer rowIndex = context.readRowHolder().getRowIndex();
        return rowIndex == null ? -1 : rowIndex;
    }
}
