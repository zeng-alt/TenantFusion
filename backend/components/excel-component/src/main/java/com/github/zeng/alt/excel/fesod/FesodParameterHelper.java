package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelProperties;
import org.apache.fesod.sheet.metadata.AbstractParameterBuilder;

/**
 * 把 {@link ExcelProperties} 里的全局项刷到 fesod builder 上，内部便利类。
 * <p>
 * 读、写、模板填充三条链都要做同一件事，抽出来避免三处复制。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
final class FesodParameterHelper {

    private FesodParameterHelper() {
    }

    /**
     * 应用全局参数。
     *
     * @param builder    fesod 读或写的 builder
     * @param properties 组件配置
     */
    @SuppressWarnings("rawtypes")
    static void applyGlobal(AbstractParameterBuilder builder, ExcelProperties properties) {
        // 逐句调用而非链式：AbstractParameterBuilder 的自反型参在通配符下会退化成原始类型，
        // 链式写法编译不过
        builder.autoTrim(properties.isAutoTrim());
        builder.use1904windowing(properties.isUse1904windowing());
        builder.filedCacheLocation(properties.getFieldCacheLocation());
        if (properties.getLocale() != null) {
            builder.locale(properties.getLocale());
        }
    }
}
