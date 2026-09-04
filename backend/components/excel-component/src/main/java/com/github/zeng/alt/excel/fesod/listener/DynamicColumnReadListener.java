package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import com.github.zeng.alt.excel.support.ExcelRowBinder;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import org.apache.fesod.sheet.context.AnalysisContext;

import java.util.Map;
import java.util.Set;

/**
 * 动态列读监听器：能对上实体字段的列照常绑定，其余列变成 {@link DynamicCell}
 * 挂到 {@link DynamicColumn#addDynamicCell(DynamicCell)} 上。
 * <p>
 * 与旧实现的区别：不再用 {@code GenericTypeResolver} 去猜单元格泛型、也不再
 * 反射调 setter 硬塞字符串——单元格类型固定为 {@link DynamicCell}，
 * 字段绑定复用 {@link ExcelRowBinder}（走 {@code ConversionService}，
 * 因此 {@code Integer}、{@code LocalDate} 这类固定列不再被当成字符串写坏）。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class DynamicColumnReadListener<T extends DynamicColumn<DynamicCell>>
        extends AbstractExcelReadListener<Map<Integer, String>, T> {

    private final ExcelRowBinder<T> binder;
    private Set<Integer> fixedColumns = Set.of();

    /**
     * @param binder    固定列的绑定器
     * @param sink      成功行的去处
     * @param options   行为开关
     * @param validator 校验器，可为 {@code null}
     */
    public DynamicColumnReadListener(ExcelRowBinder<T> binder, ExcelRowSink<T> sink,
                                     ExcelReadOptions options, ExcelRowValidator validator) {
        super(sink, options, validator);
        this.binder = binder;
    }

    @Override
    protected void onHead(Map<Integer, String> headerByColumn, AnalysisContext context) {
        binder.bindHead(headerByColumn);
        this.fixedColumns = binder.getMappedColumns();
    }

    @Override
    protected Either<String, T> toRow(Map<Integer, String> raw, AnalysisContext context) {
        return binder.bind(raw).peek(row -> attachDynamicCells(row, raw));
    }

    private void attachDynamicCells(T row, Map<Integer, String> raw) {
        Map<Integer, String> headers = getHeaders();
        raw.forEach((column, value) -> {
            if (fixedColumns.contains(column)) {
                return;
            }
            String header = headers.getOrDefault(column, "");
            row.addDynamicCell(DynamicCell.of(column, header, ExcelMessageHelper.resolve(header), value));
        });
    }
}
