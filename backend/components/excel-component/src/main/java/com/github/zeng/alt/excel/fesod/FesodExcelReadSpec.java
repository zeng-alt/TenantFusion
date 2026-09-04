package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelBindingMode;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.exception.ExcelValidationException;
import com.github.zeng.alt.excel.fesod.listener.AbstractExcelReadListener;
import com.github.zeng.alt.excel.fesod.listener.CollectingRowSink;
import com.github.zeng.alt.excel.fesod.listener.ConsumerRowSink;
import com.github.zeng.alt.excel.fesod.listener.DynamicColumnReadListener;
import com.github.zeng.alt.excel.fesod.listener.ExcelRowSink;
import com.github.zeng.alt.excel.fesod.listener.ModelReadListener;
import com.github.zeng.alt.excel.fesod.listener.PredicateRowSink;
import com.github.zeng.alt.excel.fesod.listener.ReflectiveModelReadListener;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelErrorReport;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelReadSummary;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.support.ExcelRowBinder;
import io.vavr.control.Try;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.enums.ReadDefaultReturnEnum;
import org.apache.fesod.sheet.read.builder.ExcelReaderBuilder;
import org.apache.fesod.sheet.read.builder.ExcelReaderSheetBuilder;
import org.apache.fesod.sheet.read.listener.ReadListener;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * {@link ExcelReadSpec} 的 fesod 实现。
 * <p>
 * 一个实例只服务一次读取：终结步骤消费掉数据源后不要复用（输入流读完即废）。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class FesodExcelReadSpec<T> implements ExcelReadSpec<T> {

    private final Class<T> type;
    private final FesodExcelContext context;
    private final boolean dynamic;

    private Object source;
    private Integer sheetNo = 0;
    private String sheetName;
    private int headRowNumber;
    private String password;
    private boolean i18nHead;
    private ExcelBindingMode binding;
    private ExcelReadOptions options;

    /**
     * @param type    行类型
     * @param context 共用协作对象
     * @param dynamic 是否按动态列读取
     */
    public FesodExcelReadSpec(Class<T> type, FesodExcelContext context, boolean dynamic) {
        this.type = type;
        this.context = context;
        this.dynamic = dynamic;
        this.headRowNumber = context.properties().getRead().getHeadRowNumber();
        this.i18nHead = context.properties().getRead().isI18nHead();
        this.binding = context.properties().getBinding();
        this.options = ExcelReadOptions.from(context.properties());
    }

    // ==================== 数据源 ====================

    @Override
    public ExcelReadSpec<T> from(InputStream inputStream) {
        this.source = inputStream;
        return this;
    }

    @Override
    public ExcelReadSpec<T> from(File file) {
        this.source = file;
        return this;
    }

    @Override
    public ExcelReadSpec<T> from(Path path) {
        this.source = path == null ? null : path.toFile();
        return this;
    }

    // ==================== 可选项 ====================

    @Override
    public ExcelReadSpec<T> sheet(int sheetNo) {
        this.sheetNo = sheetNo;
        this.sheetName = null;
        return this;
    }

    @Override
    public ExcelReadSpec<T> sheet(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    @Override
    public ExcelReadSpec<T> headRowNumber(int headRowNumber) {
        this.headRowNumber = headRowNumber;
        return this;
    }

    @Override
    public ExcelReadSpec<T> password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public ExcelReadSpec<T> validate(boolean validate) {
        this.options = options.withValidate(validate);
        return this;
    }

    @Override
    public ExcelReadSpec<T> validationGroups(Class<?>... groups) {
        this.options = options.withValidationGroups(groups);
        return this;
    }

    @Override
    public ExcelReadSpec<T> onError(ExcelErrorPolicy policy) {
        this.options = options.withPolicy(policy);
        return this;
    }

    @Override
    public ExcelReadSpec<T> maxErrors(int maxErrors) {
        this.options = options.withMaxErrors(maxErrors);
        return this;
    }

    @Override
    public ExcelReadSpec<T> i18nHead(boolean i18nHead) {
        this.i18nHead = i18nHead;
        return this;
    }

    @Override
    public ExcelReadSpec<T> binding(ExcelBindingMode binding) {
        this.binding = binding == null ? ExcelBindingMode.AUTO : binding;
        return this;
    }

    // ==================== 终结步骤 ====================

    @Override
    public ExcelReadResult<T> execute() {
        requireSource();
        CollectingRowSink<T> sink = new CollectingRowSink<>();
        AbstractExcelReadListener<?, T> listener = createListener(sink);
        Try<Void> read = Try.run(() -> doRead(listener));
        if (read.isFailure() && !isAbortSignal(read.getCause())) {
            throw new ExcelReadException("Excel 解析失败", read.getCause());
        }
        return new ExcelReadResult<>(sink.getRows(), listener.getErrors(), listener.getSummary());
    }

    @Override
    public Try<Long> consume(Consumer<T> consumer) {
        requireSource();
        ConsumerRowSink<T> sink = new ConsumerRowSink<>(consumer);
        return runAndCount(createListener(sink));
    }

    @Override
    public Try<Long> consumeWhile(Predicate<T> consumer) {
        requireSource();
        PredicateRowSink<T> sink = new PredicateRowSink<>(consumer);
        return runAndCount(createListener(sink));
    }

    /**
     * 逐行消费的公共收尾。
     * <p>
     * 这两个终结步骤没有地方承载失败明细，所以整单驳回只能表达成
     * {@code Try.failure(ExcelValidationException)}——异常里带着完整报告，
     * 调用方要明细就从 {@code getReport()} 取。
     *
     * @param listener 读监听器
     * @return 消费的行数，或失败
     */
    private Try<Long> runAndCount(AbstractExcelReadListener<?, T> listener) {
        Try<Void> read = Try.run(() -> doRead(listener));
        if (read.isFailure() && !isAbortSignal(read.getCause())) {
            return Try.failure(new ExcelReadException("Excel 解析失败", read.getCause()));
        }
        ExcelReadSummary summary = listener.getSummary();
        if (summary.aborted()) {
            return Try.failure(new ExcelValidationException(ExcelErrorReport.of(
                    null, new ExcelReadResult<T>(List.of(), listener.getErrors(), summary))));
        }
        return Try.success(listener.getRowCount());
    }

    // ==================== 内部 ====================

    private void doRead(ReadListener<?> listener) {
        ExcelReaderBuilder builder = createBuilder(listener);
        ExcelReaderSheetBuilder sheetBuilder = sheetName == null
                ? builder.sheet(sheetNo)
                : builder.sheet(sheetName);
        sheetBuilder.doRead();
    }

    private ExcelReaderBuilder createBuilder(ReadListener<?> listener) {
        ExcelReaderBuilder builder = FesodSheet.read();
        FesodParameterHelper.applyGlobal(builder, context.properties());
        // 贡献者先应用，组件默认值最后应用——首次匹配生效的场景下顺序是承重的
        context.readCustomizers().orderedStream().forEach(customizer -> customizer.customize(builder));
        applySource(builder);
        builder.headRowNumber(headRowNumber);
        builder.useScientificFormat(context.properties().isUseScientificFormat());
        builder.ignoreEmptyRow(context.properties().getRead().isIgnoreEmptyRow());
        builder.password(password);
        if (usesRawStringRows()) {
            // 无模型读取：fesod 把每行转成 Map<列下标, 字符串>，绑定由本组件接手
            builder.readDefaultReturn(ReadDefaultReturnEnum.STRING);
        } else {
            builder.head(type);
        }
        builder.registerReadListener(listener);
        return builder;
    }

    private void applySource(ExcelReaderBuilder builder) {
        if (source instanceof InputStream inputStream) {
            builder.file(inputStream);
        } else if (source instanceof File file) {
            builder.file(file);
        }
    }

    /**
     * 数据源缺失是调用方的编程错误，在终结步骤入口就抛出——不能等到
     * {@code Try} 里，那会被 {@link #isAbortSignal} 当成坏行中止信号吞掉。
     */
    private void requireSource() {
        if (source == null) {
            throw new ExcelReadException("未指定数据源，请先调用 from(...)");
        }
    }

    /**
     * 是否走 fesod 的无模型路径（每行给一个 {@code Map<列下标, 字符串>}）。
     * <p>
     * 动态列必然是无模型的；国际化表头匹配与 reflective 绑定都由本组件自己绑，
     * 也需要无模型。只有 engine 绑定的普通读取才让 fesod 自己建模型。
     */
    private boolean usesRawStringRows() {
        return dynamic || i18nHead || binding.isReflective();
    }

    @SuppressWarnings("unchecked")
    private AbstractExcelReadListener<?, T> createListener(ExcelRowSink<T> sink) {
        if (dynamic) {
            // read 入口已由 ExcelTemplate#readDynamic 约束了 T 的上界，这里的转型是安全的
            ExcelRowBinder<DynamicColumn<DynamicCell>> binder =
                    new ExcelRowBinder<>((Class<DynamicColumn<DynamicCell>>) type, context.conversionService());
            return (AbstractExcelReadListener<?, T>) new DynamicColumnReadListener<>(
                    binder, (ExcelRowSink<DynamicColumn<DynamicCell>>) sink, options, context.validator());
        }
        if (usesRawStringRows()) {
            return new ReflectiveModelReadListener<>(
                    new ExcelRowBinder<>(type, context.conversionService()), sink, options, context.validator());
        }
        return new ModelReadListener<>(sink, options, context.validator(), type);
    }

    /**
     * 是否是 {@code FAIL_FAST} 策略下本组件主动抛出的中止信号。
     * <p>
     * fesod 会把监听器抛出的异常包一层，所以要顺着 cause 链找。中止信号用专门的
     * {@link ExcelReadAbort} 类型，不和「真的解析炸了」混在一起。
     */
    private static boolean isAbortSignal(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof ExcelReadAbort) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }
}
