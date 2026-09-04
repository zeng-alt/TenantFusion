package com.github.zeng.alt.excel.listener;


import org.apache.fesod.sheet.context.AnalysisContext;
import com.github.zeng.alt.excel.utils.ValidaHelper;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月04日 09:20
 */
public interface ValidaReadListener<T> {

    default void valida(T t, AnalysisContext analysisContext) {
        ValidaHelper.validate(t, analysisContext);
    }
}
