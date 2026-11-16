package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.context.ExcelContextHandler;
import com.github.zeng.alt.excel.context.ExcelContextTemplate;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelFillSpec;
import com.github.zeng.alt.excel.write.ExcelErrorWriteSpec;
import com.github.zeng.alt.excel.read.ExcelErrorReport;
import com.github.zeng.alt.excel.poi.PoiExcelErrorWriteSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * {@link ExcelTemplate} 的 fesod 实现。
 * <p>
 * 本类是无状态的，每个入口现场造一段链式配置面；可变配置全部落在那段链上，
 * 因此单例 bean 可以被并发使用。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class FesodExcelTemplate implements ExcelTemplate {

    private final FesodExcelContext context;
    private final Supplier<ExcelContextHandler.Snapshot> contextFactory;
    private final Consumer<ExcelErrorWriteSpec> errorCustomizer;

    /** 非 Spring 场景使用可用的内置上下文贡献者。 */
    public FesodExcelTemplate(FesodExcelContext context) {
        this(context, () -> context.properties().getContext().isEnabled()
                ? ExcelContextTemplate.captureDefaults() : ExcelContextHandler.Snapshot.empty(), spec -> { });
    }

    @Override
    public ExcelContextHandler.Snapshot captureContext() {
        return contextFactory.get();
    }

    @Override
    public <T> ExcelReadSpec<T> read(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelReadSpec<>(type, context, false, captureContext());
    }

    @Override
    public <T extends DynamicColumn<DynamicCell>> ExcelReadSpec<T> readDynamic(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelReadSpec<>(type, context, true, captureContext());
    }

    @Override
    public <T> ExcelWriteSpec<T> write(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelWriteSpec<>(type, null, context);
    }

    @Override
    public ExcelWriteSpec<List<Object>> writeHead(List<List<String>> head) {
        Assert.notEmpty(head, "表头不能为空");
        return new FesodExcelWriteSpec<>(null, head, context);
    }

    @Override
    public ExcelFillSpec fill(String templateLocation) {
        Assert.hasText(templateLocation, "模板路径不能为空");
        return new FesodExcelFillSpec(templateLocation, context);
    }

    @Override
    public ExcelErrorWriteSpec writeErrors(ExcelErrorReport report) {
        Assert.notNull(report, "错误报告不能为空");
        final ExcelErrorWriteSpec spec = new PoiExcelErrorWriteSpec(report, context.properties().getErrorReport());
        errorCustomizer.accept(spec);
        return spec;
    }
}
