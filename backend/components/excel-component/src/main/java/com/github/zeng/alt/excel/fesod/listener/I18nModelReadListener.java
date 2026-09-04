package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import org.apache.fesod.sheet.context.AnalysisContext;

import java.util.Map;

/**
 * 国际化表头读监听器：以「无模型」方式拿到每行的字符串值，再由
 * {@link I18nHeadBinder} 按国际化后的表头文本绑定到实体。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class I18nModelReadListener<T> extends AbstractExcelReadListener<Map<Integer, String>, T> {

    private final I18nHeadBinder<T> binder;

    /**
     * @param binder    表头绑定器
     * @param sink      成功行的去处
     * @param options   行为开关
     * @param validator 校验器，可为 {@code null}
     */
    public I18nModelReadListener(I18nHeadBinder<T> binder, ExcelRowSink<T> sink,
                                 ExcelReadOptions options, ExcelRowValidator validator) {
        super(sink, options, validator);
        this.binder = binder;
    }

    @Override
    protected void onHead(Map<Integer, String> headerByColumn, AnalysisContext context) {
        binder.bindHead(headerByColumn);
    }

    @Override
    protected Either<String, T> toRow(Map<Integer, String> raw, AnalysisContext context) {
        if (binder.isUnmapped()) {
            return Either.left("表头与实体字段无一列匹配，请确认模板是否正确");
        }
        return binder.bind(raw);
    }
}
