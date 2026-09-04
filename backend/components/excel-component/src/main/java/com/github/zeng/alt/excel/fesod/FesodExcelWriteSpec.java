package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelBindingMode;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.fesod.handler.I18nHeadWriteHandler;
import com.github.zeng.alt.excel.support.ExcelFieldMeta;
import com.github.zeng.alt.excel.support.ExcelRowAccessor;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Try;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
    private ExcelBindingMode binding;
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
        this.binding = context.properties().getBinding();
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

    @Override
    public ExcelWriteSpec<T> binding(ExcelBindingMode binding) {
        this.binding = binding == null ? ExcelBindingMode.AUTO : binding;
        return this;
    }

    // ==================== 终结步骤 ====================

    @Override
    public Try<Long> write(Collection<T> rows) {
        return Try.of(() -> doWrite(rows == null ? List.of() : rows));
    }

    @Override
    public Try<Long> write(Iterator<T> rows) {
        return rows == null ? write(List.of()) : Try.of(() -> doWriteBatched(rows));
    }

    // ==================== 内部 ====================

    private long doWrite(Collection<T> rows) {
        try (ExcelWriter writer = createBuilder().build()) {
            writer.write(toWriterRows(rows), createSheet());
            return rows.size();
        }
    }

    /**
     * 分批从游标拉数据写出，内存占用与总行数无关。
     *
     * @param rows 数据游标
     * @return 写出的行数
     */
    private long doWriteBatched(Iterator<T> rows) {
        int batchSize = Math.max(1, context.properties().getWrite().getBatchSize());
        long count = 0L;
        try (ExcelWriter writer = createBuilder().build()) {
            WriteSheet sheet = createSheet();
            List<T> batch = new ArrayList<>(batchSize);
            while (rows.hasNext()) {
                batch.add(rows.next());
                if (batch.size() == batchSize) {
                    writer.write(toWriterRows(batch), sheet);
                    count += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                writer.write(toWriterRows(batch), sheet);
                count += batch.size();
            }
        }
        return count;
    }

    /**
     * 交给 fesod 的行集合。
     * <p>
     * engine 绑定原样递实体，由 fesod 自己反射；reflective 绑定先把实体拆成值列表，
     * 走 fesod 的无模型写出路径（行是 {@code Collection}），绕开它那条用 cglib
     * 运行期生成字节码的实体路径——native image 不支持运行期生成字节码。
     */
    private Collection<?> toWriterRows(Collection<T> rows) {
        if (!usesReflectiveBinding()) {
            return rows;
        }
        ExcelRowAccessor<T> accessor = ExcelRowAccessor.of(type);
        List<ExcelFieldMeta> selected = accessor.selectFields(includeColumns, excludeColumns);
        List<List<Object>> raw = new ArrayList<>(rows.size());
        for (T row : rows) {
            raw.add(accessor.extract(row, selected));
        }
        return raw;
    }

    /** 是否由本组件自己做实体绑定：有实体类型、无运行期表头、绑定方式落到 reflective */
    private boolean usesReflectiveBinding() {
        return type != null && head == null && binding.isReflective();
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
            return;
        }
        if (usesReflectiveBinding()) {
            // 表头由 accessor 从 @ExcelProperty 算出，i18n 仍由 I18nHeadWriteHandler 处理
            ExcelRowAccessor<T> accessor = ExcelRowAccessor.of(type);
            builder.head(accessor.head(accessor.selectFields(includeColumns, excludeColumns)));
            return;
        }
        if (type != null) {
            builder.head(type);
        }
    }

    private void applyColumnFilter(ExcelWriterBuilder builder) {
        if (usesReflectiveBinding()) {
            // reflective 绑定下 fesod 看到的是无模型行，按字段名筛列无从下手，
            // 筛选已经在 accessor.selectFields 里做完了
            return;
        }
        if (includeColumns != null && !includeColumns.isEmpty()) {
            builder.includeColumnFieldNames(includeColumns);
            return;
        }
        if (excludeColumns != null && !excludeColumns.isEmpty()) {
            builder.excludeColumnFieldNames(excludeColumns);
        }
    }
}
