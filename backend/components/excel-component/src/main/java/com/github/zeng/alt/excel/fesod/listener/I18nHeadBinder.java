package com.github.zeng.alt.excel.fesod.listener;

import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 按国际化后的表头文本把列绑定到实体字段。
 * <p>
 * 解决的问题：{@code @ExcelProperty("{user.name}")} 声明的是 i18n key，而用户上传的
 * 文件表头写的是「姓名」，fesod 按字面量匹配对不上。做法是先把实体上每个字段的
 * key 解析成当前 Locale 的文本，建立「表头文本 → 字段名」映射，再用表头行定位列，
 * 最后用 Spring {@code ConversionService} 把字符串值转成字段类型。
 * <p>
 * 代价是不走 fesod 的自定义 {@code Converter}，所以这条路径是可选的
 * （{@code ExcelReadSpec#i18nHead(boolean)}），默认关闭。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class I18nHeadBinder<T> {

    private final Class<T> type;
    private final ConversionService conversionService;
    private final Map<String, String> fieldByHeader;
    private Map<Integer, String> fieldByColumn = Map.of();

    /**
     * @param type              行类型
     * @param conversionService 字符串 → 字段类型的转换服务
     */
    public I18nHeadBinder(Class<T> type, ConversionService conversionService) {
        this.type = type;
        this.conversionService = conversionService;
        this.fieldByHeader = buildFieldByHeader(type);
    }

    /**
     * 用表头行建立列 → 字段的映射，每次读新工作表时调用一次。
     *
     * @param headerByColumn 列下标 → 表头文本
     */
    public void bindHead(Map<Integer, String> headerByColumn) {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        headerByColumn.forEach((column, header) -> {
            String field = fieldByHeader.get(normalize(header));
            if (field != null) {
                mapping.put(column, field);
            }
        });
        this.fieldByColumn = mapping;
    }

    /**
     * 表头行是否一列都没对上，用于给出「表头不匹配」这种整份文件级别的提示。
     *
     * @return true 表示没有任何列能对上字段
     */
    public boolean isUnmapped() {
        return fieldByColumn.isEmpty();
    }

    /**
     * 已对上实体字段的列下标，动态列读取时用来判断哪些列该进
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
                .mapLeft(e -> e.getMessage() == null
                        ? "行数据无法绑定到 " + type.getSimpleName()
                        : e.getMessage());
    }

    private T doBind(Map<Integer, String> valueByColumn) {
        T target = BeanUtils.instantiateClass(type);
        BeanWrapper wrapper = new BeanWrapperImpl(target);
        wrapper.setConversionService(conversionService);
        fieldByColumn.forEach((column, field) -> {
            String text = valueByColumn.get(column);
            if (text != null && !text.isBlank() && wrapper.isWritableProperty(field)) {
                wrapper.setPropertyValue(field, text.strip());
            }
        });
        return target;
    }

    private static Map<String, String> buildFieldByHeader(Class<?> type) {
        Map<String, String> result = new HashMap<>();
        ReflectionUtils.doWithFields(type, field -> registerField(result, field), I18nHeadBinder::isMappable);
        return Map.copyOf(result);
    }

    private static void registerField(Map<String, String> target, Field field) {
        ExcelProperty property = field.getAnnotation(ExcelProperty.class);
        String head = leafHead(property, field.getName());
        // 同时登记 i18n key 原文与解析后的文本：两种表头的文件都能读
        target.putIfAbsent(normalize(head), field.getName());
        target.putIfAbsent(normalize(ExcelMessageHelper.resolve(head)), field.getName());
        target.putIfAbsent(normalize(field.getName()), field.getName());
    }

    private static String leafHead(ExcelProperty property, String fallback) {
        if (property == null || property.value().length == 0) {
            return fallback;
        }
        String[] heads = property.value();
        String leaf = heads[heads.length - 1];
        return leaf == null || leaf.isBlank() ? fallback : leaf;
    }

    private static boolean isMappable(Field field) {
        return !field.isSynthetic()
                && !Modifier.isStatic(field.getModifiers())
                && !Modifier.isFinal(field.getModifiers())
                && field.getAnnotation(ExcelIgnore.class) == null;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip();
    }
}
