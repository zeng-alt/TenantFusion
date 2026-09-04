package com.github.zeng.alt.excel.support;

import io.vavr.control.Either;
import io.vavr.control.Try;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 按表头文本把「列下标 → 字符串」的一行绑定到实体。
 * <p>
 * 每次读取新建一个实例（持有当前工作表的列映射），但字段元数据来自全局缓存的
 * {@link ExcelRowAccessor}，所以反射查找不会随读取次数增长。
 * <p>
 * 表头匹配同时接受三种写法，导出模板填回来、手写表头、按 i18n key 写的表头都能读：
 * {@code @ExcelProperty} 的原文（可能是 {@code {i18n.key}}）、它解析后的当前语言文本、
 * 以及字段名本身。
 * <p>
 * 值转换走 Spring {@code ConversionService}，因此不支持 fesod 的自定义
 * {@code Converter}——这是换取 native 兼容性的代价，见 {@link ExcelRowAccessor}。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelRowBinder<T> {

    private final ExcelRowAccessor<T> accessor;
    private final ConversionService conversionService;
    private final Map<String, ExcelFieldMeta> fieldByHeader;
    private Map<Integer, ExcelFieldMeta> fieldByColumn = Map.of();

    /**
     * @param type              行类型
     * @param conversionService 字符串 → 字段类型的转换服务
     */
    public ExcelRowBinder(Class<T> type, ConversionService conversionService) {
        this.accessor = ExcelRowAccessor.of(type);
        this.conversionService = conversionService;
        this.fieldByHeader = buildAliases(accessor);
    }

    /**
     * 用表头行建立列 → 字段的映射，每读一张新工作表调用一次。
     *
     * @param headerByColumn 列下标 → 表头文本
     */
    public void bindHead(Map<Integer, String> headerByColumn) {
        Map<Integer, ExcelFieldMeta> mapping = new LinkedHashMap<>();
        headerByColumn.forEach((column, header) -> {
            ExcelFieldMeta field = fieldByHeader.get(normalize(header));
            if (field != null) {
                mapping.put(column, field);
            }
        });
        this.fieldByColumn = mapping;
    }

    /**
     * 表头行是否一列都没对上，用于给出「模板不对」这种整份文件级别的提示。
     *
     * @return true 表示没有任何列能对上字段
     */
    public boolean isUnmapped() {
        return fieldByColumn.isEmpty();
    }

    /**
     * 已对上实体字段的列下标；动态列读取据此判断哪些列该进
     * {@link com.github.zeng.alt.excel.dynamic.DynamicCell}。
     *
     * @return 列下标集合
     */
    public Set<Integer> getMappedColumns() {
        return Collections.unmodifiableSet(fieldByColumn.keySet());
    }

    /**
     * 绑定一行。
     *
     * @param valueByColumn 列下标 → 单元格文本
     * @return 成功为 {@code right(实例)}，转换失败为 {@code left(原因)}
     */
    public Either<String, T> bind(Map<Integer, String> valueByColumn) {
        return Try.of(() -> doBind(valueByColumn))
                .toEither()
                .mapLeft(this::describe);
    }

    private T doBind(Map<Integer, String> valueByColumn) {
        T target = accessor.instantiate();
        fieldByColumn.forEach((column, field) -> {
            String text = valueByColumn.get(column);
            if (text != null && !text.isBlank()) {
                accessor.write(target, field, convert(text.strip(), field));
            }
        });
        return target;
    }

    private Object convert(String text, ExcelFieldMeta field) {
        if (field.propertyType() == String.class) {
            return text;
        }
        return conversionService.convert(text,
                TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(field.propertyType()));
    }

    private String describe(Throwable cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? "行数据无法绑定到 " + accessor.getType().getSimpleName()
                : message;
    }

    private static <T> Map<String, ExcelFieldMeta> buildAliases(ExcelRowAccessor<T> accessor) {
        Map<String, ExcelFieldMeta> aliases = new HashMap<>();
        for (ExcelFieldMeta field : accessor.getFields()) {
            aliases.putIfAbsent(normalize(field.headCode()), field);
            aliases.putIfAbsent(normalize(ExcelMessageHelper.resolve(field.headCode())), field);
            aliases.putIfAbsent(normalize(field.fieldName()), field);
        }
        return Map.copyOf(aliases);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip();
    }
}
