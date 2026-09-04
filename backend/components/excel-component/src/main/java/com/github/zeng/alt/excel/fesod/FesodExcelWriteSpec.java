package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.fesod.handler.I18nHeadWriteHandler;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * {@link ExcelWriteSpec} 的 fesod 实现。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class FesodExcelWriteSpec<T> implements ExcelWriteSpec<T> {

    private final Class<T> type;
    private final List<List<String>> head;
    private final FesodExcelContext context;

    private Object target;
    private String sheetName;
    private String password;
    private boolean autoWidth;
    private boolean i18nHead;
    private boolean autoCloseStream;
    private Collection<String> includeColumns;
    private Collection<String> excludeColumns;

    /**
     * @param type    行类型；无实体导出时为 {@code null}
     * @param head    运行期表头；按实体导出时为 {@code null}
     * @param context 共用协作对象
     */
    public FesodExcelWriteSpec(Class<T> type, List<List<String>> head, FesodExcelContext context) {
        this.type = type;
        this.head = head;
        this.context = context;
        this.autoWidth = context.properties().getWrite().isAutoWidth();
        this.i18nHead = context.properties().getWrite().isI18nHead();
    }

    // ==================== 输出目标 ====================

    @Override
    public ExcelWriteSpec<T> to(OutputStream outputStream) {
        this.target = outputStream;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> to(File file) {
        this.target = file;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> to(Path path) {
        this.target = path == null ? null : path.toFile();
        return this;
    }

    // ==================== 可选项 ====================

    @Override
    public ExcelWriteSpec<T> sheet(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> autoWidth(boolean autoWidth) {
        this.autoWidth = autoWidth;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> i18nHead(boolean i18nHead) {
        this.i18nHead = i18nHead;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> includeColumns(Collection<String> fieldNames) {
        this.includeColumns = fieldNames;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> excludeColumns(Collection<String> fieldNames) {
        this.excludeColumns = fieldNames;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public ExcelWriteSpec<T> autoCloseStream(boolean autoCloseStream) {
        this.autoCloseStream = autoCloseStream;
        return this;
    }

    // ==================== 终结步骤 ====================

    @Override
    public Try<Long> write(Collection<T> rows) {
        return Try.of(() -> doWrite(rows == null ? List.of() : rows));
    }

    @Override
    public Try<Long> write(Flowable<T> rows) {
        if (rows == null) {
            return write(List.of());
        }
        int batchSize = context.properties().getWrite().getBatchSize();
        // 写文件是同步动作，这里的阻塞发生在链路最外层的终结步骤，不藏在中间操作符里
        return Try.of(() -> doWriteBatched(rows.buffer(batchSize).blockingIterable()));
    }

    // ==================== 内部 ====================

    private long doWrite(Collection<T> rows) {
        try (ExcelWriter writer = createBuilder().build()) {
            writer.write(rows, createSheet());
            return rows.size();
        }
    }

    private long doWriteBatched(Iterable<List<T>> batches) {
        long count = 0L;
        try (ExcelWriter writer = createBuilder().build()) {
            WriteSheet sheet = createSheet();
            for (List<T> batch : batches) {
                writer.write(batch, sheet);
                count += batch.size();
            }
        }
        return count;
    }

    private WriteSheet createSheet() {
        return sheetName == null
                ? FesodSheet.writerSheet().build()
                : FesodSheet.writerSheet(sheetName).build();
    }

    private ExcelWriterBuilder createBuilder() {
        ExcelWriterBuilder builder = FesodSheet.write();
        FesodParameterHelper.applyGlobal(builder, context.properties());
        // 贡献者先应用，组件默认值最后应用
        context.writeCustomizers().orderedStream().forEach(customizer -> customizer.customize(builder));
        applyTarget(builder);
        applyHead(builder);
        applyColumnFilter(builder);
        builder.autoCloseStream(autoCloseStream);
        builder.inMemory(context.properties().getWrite().isInMemory());
        builder.password(password);
        if (i18nHead) {
            builder.registerWriteHandler(new I18nHeadWriteHandler());
        }
        if (autoWidth) {
            builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        }
        return builder;
    }

    private void applyTarget(ExcelWriterBuilder builder) {
        if (target instanceof OutputStream outputStream) {
            builder.file(outputStream);
        } else if (target instanceof File file) {
            builder.file(file);
        } else {
            throw new ExcelWriteException("未指定输出目标，请先调用 to(...)");
        }
    }

    private void applyHead(ExcelWriterBuilder builder) {
        if (head != null) {
            builder.head(head);
        } else if (type != null) {
            builder.head(type);
        }
    }

    private void applyColumnFilter(ExcelWriterBuilder builder) {
        if (includeColumns != null && !includeColumns.isEmpty()) {
            builder.includeColumnFieldNames(includeColumns);
            return;
        }
        if (excludeColumns != null && !excludeColumns.isEmpty()) {
            builder.excludeColumnFieldNames(excludeColumns);
        }
    }
}
