package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.fesod.ExcelReadOptions;
import com.github.zeng.alt.excel.support.ExcelRowBinder;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import io.vavr.control.Either;
import org.apache.fesod.sheet.context.AnalysisContext;

import java.util.Map;

/**
 * 反射绑定读监听器：以「无模型」方式拿到每行的字符串值，再由 {@link ExcelRowBinder}
 * 按表头文本绑定到实体。
 * <p>
 * 这条路径不碰 fesod 的 cglib 实体绑定，是 native image 下唯一可用的实体读法，
 * 同时也是国际化表头匹配的实现方式。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ReflectiveModelReadListener<T> extends AbstractExcelReadListener<Map<Integer, String>, T> {

    private final ExcelRowBinder<T> binder;

    /**
     * @param binder    行绑定器
     * @param sink      成功行的去处
     * @param options   行为开关
     * @param validator 校验器，可为 {@code null}
     */
    public ReflectiveModelReadListener(ExcelRowBinder<T> binder, ExcelRowSink<T> sink,
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
