package com.github.zeng.alt.excel.fesod;

import org.apache.fesod.sheet.read.builder.ExcelReaderBuilder;

/**
 * 读取 builder 的扩展点。
 * <p>
 * 下游模块声明本接口的 bean 就能给「所有」读取操作追加配置——注册自定义
 * {@code Converter}、打开 {@code extraRead}、换 {@code ReadCache} 之类。
 * 组件用 {@code ObjectProvider.orderedStream()} 收集，因此零贡献者也能工作，
 * 且尊重 {@code @Order}；贡献者先应用，组件默认值最后应用。
 * <p>
 * 这是本模块唯一暴露引擎类型的地方：链式配置面（{@code ExcelReadSpec}）保持引擎无关，
 * 确实需要摸底层 builder 时走这里。
 * <pre>{@code
 * @Bean
 * @Order(0)
 * ExcelReadCustomizer moneyConverterCustomizer() {
 *     return builder -> builder.registerConverter(new MoneyConverter());
 * }
 * }</pre>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@FunctionalInterface
public interface ExcelReadCustomizer {

    /**
     * 定制读取 builder。
     *
     * @param builder fesod 读取 builder
     */
    void customize(ExcelReaderBuilder builder);
}
