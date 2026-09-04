package com.github.zeng.alt.excel.fesod;

import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;

/**
 * 写出 builder 的扩展点。
 * <p>
 * 与 {@link ExcelReadCustomizer} 对称：下游声明本接口的 bean，即可给所有写出操作
 * 追加 {@code WriteHandler}（统一样式、水印、冻结首行）或自定义 {@code Converter}。
 * 组件用 {@code ObjectProvider.orderedStream()} 收集，贡献者先应用、组件默认值最后应用。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@FunctionalInterface
public interface ExcelWriteCustomizer {

    /**
     * 定制写出 builder。
     *
     * @param builder fesod 写出 builder
     */
    void customize(ExcelWriterBuilder builder);
}
