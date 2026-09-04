package com.github.zeng.alt.excel.support;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * 一个 Excel 列对应的字段元数据，由 {@link ExcelRowAccessor} 在每个类型上解析一次并缓存。
 * <p>
 * 反射（读注解、找 getter/setter）只发生在构造本记录的那一刻；之后逐行读写复用
 * 这里的 {@link Method} 句柄，不再有任何查找动作。
 *
 * @param fieldName    字段名
 * @param headCode     {@code @ExcelProperty} 上声明的叶子表头，可能是 {@code {i18n.key}}
 * @param index        {@code @ExcelProperty#index()}，{@code -1} 表示未指定
 * @param order        {@code @ExcelProperty#order()}
 * @param declaration  字段在类里的声明序号，用于兜底排序
 * @param propertyType 字段类型
 * @param readMethod   getter，没有则为 {@code null}（该字段不参与导出）
 * @param writeMethod  setter，没有则为 {@code null}（该字段不参与导入）
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelFieldMeta(
        String fieldName,
        String headCode,
        int index,
        int order,
        int declaration,
        Class<?> propertyType,
        Method readMethod,
        Method writeMethod) {

    /** {@code @ExcelProperty#index()} 未指定时的取值 */
    public static final int NO_INDEX = -1;

    /**
     * 列顺序比较器：显式 {@code index} 优先（绝对位置），其次 {@code order}，
     * 最后按字段声明顺序。与 fesod 的排序规则对齐，保证 reflective 绑定导出的
     * 列顺序和 engine 绑定一致。
     */
    public static final Comparator<ExcelFieldMeta> BY_COLUMN = Comparator
            .comparingInt((ExcelFieldMeta meta) -> meta.index() >= 0 ? meta.index() : Integer.MAX_VALUE)
            .thenComparingInt(ExcelFieldMeta::order)
            .thenComparingInt(ExcelFieldMeta::declaration);
}
