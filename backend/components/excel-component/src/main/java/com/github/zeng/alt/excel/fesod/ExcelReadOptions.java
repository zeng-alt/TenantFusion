package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;

/**
 * 读监听器需要的行为开关，内部参数对象。
 * <p>
 * 单独抽出来是为了让监听器的构造器保持在 4 个参数以内，也避免把整个
 * {@link ExcelProperties} 递给监听器。
 *
 * @param validate         是否执行 Bean Validation
 * @param validationGroups 校验分组，空数组表示默认分组（{@code Default.class}）
 * @param policy           坏行策略：跳过、马上中断、还是校验完整个文件再中断
 * @param maxErrors        失败明细上限，达到后停止解析并标记截断
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelReadOptions(
        boolean validate, Class<?>[] validationGroups, ExcelErrorPolicy policy, int maxErrors) {

    /** 未指定分组时用的空数组，jakarta 的 Validator 收到空数组即按 Default 分组校验 */
    private static final Class<?>[] NO_GROUPS = new Class<?>[0];

    /**
     * 取配置里的默认值。
     *
     * @param properties 组件配置
     * @return 选项
     */
    public static ExcelReadOptions from(ExcelProperties properties) {
        ExcelProperties.Read read = properties.getRead();
        return new ExcelReadOptions(read.isValidate(), NO_GROUPS, read.getOnError(), read.getMaxErrors());
    }

    /**
     * 替换校验开关。
     *
     * @param value 新值
     * @return 新选项
     */
    public ExcelReadOptions withValidate(boolean value) {
        return new ExcelReadOptions(value, validationGroups, policy, maxErrors);
    }

    /**
     * 替换校验分组。传入非空分组会同时把校验开关打开——指定了分组却不校验没有意义。
     *
     * @param value 新值，{@code null} 视为空数组
     * @return 新选项
     */
    public ExcelReadOptions withValidationGroups(Class<?>[] value) {
        Class<?>[] groups = value == null ? NO_GROUPS : value.clone();
        return new ExcelReadOptions(validate || groups.length > 0, groups, policy, maxErrors);
    }

    /**
     * 替换坏行策略。
     *
     * @param value 新值，{@code null} 忽略
     * @return 新选项
     */
    public ExcelReadOptions withPolicy(ExcelErrorPolicy value) {
        return value == null ? this : new ExcelReadOptions(validate, validationGroups, value, maxErrors);
    }

    /**
     * 替换失败明细上限。
     *
     * @param value 新值，非正数忽略
     * @return 新选项
     */
    public ExcelReadOptions withMaxErrors(int value) {
        return value <= 0 ? this : new ExcelReadOptions(validate, validationGroups, policy, value);
    }

    /**
     * 校验分组的防御性副本——record 的数组字段是可变的，直接返回会让调用方改到内部状态。
     *
     * @return 分组数组的副本
     */
    @Override
    public Class<?>[] validationGroups() {
        return validationGroups.clone();
    }
}
