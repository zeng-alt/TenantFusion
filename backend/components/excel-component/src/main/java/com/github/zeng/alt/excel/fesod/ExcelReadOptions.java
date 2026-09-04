package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelProperties;

/**
 * 读监听器需要的行为开关，内部参数对象。
 * <p>
 * 单独抽出来是为了让监听器的构造器保持在 4 个参数以内，也避免把整个
 * {@link ExcelProperties} 递给监听器。
 *
 * @param validate        是否执行 Bean Validation
 * @param skipInvalidRows 坏行是否跳过；{@code false} 时首个坏行即中止解析
 * @param maxErrors       失败明细上限，达到后停止解析
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelReadOptions(boolean validate, boolean skipInvalidRows, int maxErrors) {

    /**
     * 取配置里的默认值。
     *
     * @param properties 组件配置
     * @return 选项
     */
    public static ExcelReadOptions from(ExcelProperties properties) {
        ExcelProperties.Read read = properties.getRead();
        return new ExcelReadOptions(read.isValidate(), read.isSkipInvalidRows(), read.getMaxErrors());
    }

    /**
     * 替换校验开关。
     *
     * @param value 新值
     * @return 新选项
     */
    public ExcelReadOptions withValidate(boolean value) {
        return new ExcelReadOptions(value, skipInvalidRows, maxErrors);
    }

    /**
     * 替换坏行策略。
     *
     * @param value 新值
     * @return 新选项
     */
    public ExcelReadOptions withSkipInvalidRows(boolean value) {
        return new ExcelReadOptions(validate, value, maxErrors);
    }
}
