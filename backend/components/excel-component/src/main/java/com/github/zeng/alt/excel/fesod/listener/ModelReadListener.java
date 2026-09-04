package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import org.apache.fesod.sheet.context.AnalysisContext;

/**
 * 默认读监听器：模型由 fesod 自己构建（表头按 {@code @ExcelProperty} 字面量匹配，
 * 自定义 {@code Converter} 全部生效），本类只负责校验与错误记账。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ModelReadListener<T> extends AbstractExcelReadListener<T, T> {

    public ModelReadListener(ExcelRowSink<T> sink, ExcelReadOptions options,
                             ExcelRowValidator validator, Class<T> rowType) {
        super(sink, options, validator, rowType);
    }

    @Override
    protected Either<String, T> toRow(T raw, AnalysisContext context) {
        return raw == null ? Either.left("空行") : Either.right(raw);
    }
}
