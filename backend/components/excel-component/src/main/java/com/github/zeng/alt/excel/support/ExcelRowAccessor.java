package com.github.zeng.alt.excel.support;

import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 行类型的反射元数据，一个类型解析一次、全局缓存。
 * <p>
 * <b>为什么需要它。</b>fesod 的实体绑定路径（读的
 * {@code ModelBuildEventListener#buildUserModel}、写的
 * {@code ExcelWriteAddExecutor#addJavaObjectToExcel}）用 cglib
 * {@code BeanMap.Generator.create()} 在运行期生成字节码，GraalVM native image
 * 不支持这种做法，注册再多反射 hints 也没用。而 fesod 的「无模型」路径
 * （读返回 {@code Map<列下标, 字符串>}、写接受 {@code Collection} 行）不碰 cglib。
 * 本类就是补上无模型路径缺的那一半——实体与 {@code Map}/{@code List} 之间的
 * 双向绑定，用普通反射实现，因此可以靠 AOT hints 在 native 下工作。
 * <p>
 * <b>反射用量。</b>扫字段、读注解、找 getter/setter 只在
 * {@link #of(Class)} 首次遇到该类型时发生一次，结果连同 {@link java.lang.reflect.Method}
 * 句柄一起缓存；之后逐行读写就是直接的 {@code Method#invoke}，没有任何查找。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public final class ExcelRowAccessor<T> {

    private static final Map<Class<?>, ExcelRowAccessor<?>> CACHE = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final Constructor<T> constructor;
    private final List<ExcelFieldMeta> fields;

    private ExcelRowAccessor(Class<T> type, Constructor<T> constructor, List<ExcelFieldMeta> fields) {
        this.type = type;
        this.constructor = constructor;
        this.fields = fields;
    }

    /**
     * 取（或首次解析并缓存）某个行类型的访问器。
     *
     * @param type 行类型
     * @param <T>  行类型
     * @return 访问器
     */
    @SuppressWarnings("unchecked")
    public static <T> ExcelRowAccessor<T> of(Class<T> type) {
        return (ExcelRowAccessor<T>) CACHE.computeIfAbsent(type, ExcelRowAccessor::parse);
    }

    /**
     * 按列顺序排好的字段元数据。
     *
     * @return 不可变列表
     */
    public List<ExcelFieldMeta> getFields() {
        return fields;
    }

    /**
     * 行类型。
     *
     * @return 类型
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * 按 include / exclude 筛出要导出的列，两者都为空则是全部字段。
     * <p>
     * include 优先，与 {@code ExcelWriteSpec} 的语义一致。
     *
     * @param include 只保留这些字段名
     * @param exclude 排除这些字段名
     * @return 按列顺序排好的字段
     */
    public List<ExcelFieldMeta> selectFields(Collection<String> include, Collection<String> exclude) {
        if (include != null && !include.isEmpty()) {
            return fields.stream().filter(field -> include.contains(field.fieldName())).toList();
        }
        if (exclude != null && !exclude.isEmpty()) {
            return fields.stream().filter(field -> !exclude.contains(field.fieldName())).toList();
        }
        return fields;
    }

    /**
     * 导出用的表头，内容是 {@code @ExcelProperty} 上的原文。
     * <p>
     * 这里<b>不</b>做 i18n 解析：{@code {i18n.key}} 交给
     * {@code I18nHeadWriteHandler} 在写单元格时替换，与 engine 绑定路径共用同一套
     * 逻辑，也避免把 Locale 这种请求维度的状态混进可缓存的元数据里。
     *
     * @param selected {@link #selectFields} 的结果
     * @return fesod 的 head 结构，一列一个元素
     */
    public List<List<String>> head(List<ExcelFieldMeta> selected) {
        List<List<String>> head = new ArrayList<>(selected.size());
        for (ExcelFieldMeta field : selected) {
            head.add(List.of(field.headCode()));
        }
        return head;
    }

    /**
     * 把一行实体拆成与 {@link #head(List)} 同序的值列表。
     *
     * @param row      实体
     * @param selected {@link #selectFields} 的结果
     * @return 值列表，缺 getter 的字段位置为 {@code null}
     */
    public List<Object> extract(T row, List<ExcelFieldMeta> selected) {
        List<Object> values = new ArrayList<>(selected.size());
        for (ExcelFieldMeta field : selected) {
            values.add(readValue(field, row));
        }
        return values;
    }

    /**
     * 新建一个空实体。
     *
     * @return 实体
     */
    public T instantiate() {
        if (constructor == null) {
            throw new ExcelReadException(type.getName() + " 缺少无参构造器，无法用于 Excel 导入");
        }
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ExcelReadException("无法实例化 " + type.getName(), e);
        }
    }

    /**
     * 给实体的一个字段赋值，值由调用方先转换成目标类型。
     *
     * @param row   实体
     * @param field 字段元数据
     * @param value 已转换好的值
     */
    public void write(T row, ExcelFieldMeta field, Object value) {
        if (field.writeMethod() == null) {
            return;
        }
        try {
            field.writeMethod().invoke(row, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ExcelReadException(
                    "给 %s#%s 赋值失败".formatted(type.getSimpleName(), field.fieldName()), e);
        }
    }

    private Object readValue(ExcelFieldMeta field, T row) {
        if (field.readMethod() == null) {
            return null;
        }
        try {
            return field.readMethod().invoke(row);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ExcelWriteException("读取 %s#%s 失败".formatted(type.getSimpleName(), field.fieldName()), e);
        }
    }

    // ==================== 解析（每个类型只跑一次） ====================

    private static <T> ExcelRowAccessor<T> parse(Class<T> type) {
        boolean annotatedOnly = type.getAnnotation(ExcelIgnoreUnannotated.class) != null;
        Map<String, ExcelFieldMeta> byName = new LinkedHashMap<>();
        AtomicInteger declaration = new AtomicInteger();

        ReflectionUtils.doWithFields(
                type,
                field -> collect(byName, type, field, declaration.getAndIncrement(), annotatedOnly),
                ExcelRowAccessor::isCandidate);

        List<ExcelFieldMeta> fields = new ArrayList<>(byName.values());
        fields.sort(ExcelFieldMeta.BY_COLUMN);
        return new ExcelRowAccessor<>(type, resolveConstructor(type), Collections.unmodifiableList(fields));
    }

    private static <T> void collect(Map<String, ExcelFieldMeta> target, Class<T> type,
                                    Field field, int declaration, boolean annotatedOnly) {
        ExcelProperty property = field.getAnnotation(ExcelProperty.class);
        if (annotatedOnly && property == null) {
            return;
        }
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(type, field.getName());
        if (descriptor == null) {
            return;
        }
        // getter/setter 本身是 public，但声明它们的类可能是包私有的，
        // 那样跨包反射调用会抛 IllegalAccessException——常见于嵌套的 DTO
        makeAccessible(descriptor.getReadMethod());
        makeAccessible(descriptor.getWriteMethod());
        // 父类字段先被访问到时不覆盖，与 doWithFields 的自上而下顺序无关
        target.putIfAbsent(field.getName(), new ExcelFieldMeta(
                field.getName(),
                leafHead(property, field.getName()),
                property == null ? ExcelFieldMeta.NO_INDEX : property.index(),
                property == null ? Integer.MAX_VALUE : property.order(),
                declaration,
                field.getType(),
                descriptor.getReadMethod(),
                descriptor.getWriteMethod()));
    }

    private static void makeAccessible(Method method) {
        if (method != null) {
            ReflectionUtils.makeAccessible(method);
        }
    }

    private static boolean isCandidate(Field field) {
        return !field.isSynthetic()
                && !Modifier.isStatic(field.getModifiers())
                && !Modifier.isFinal(field.getModifiers())
                && field.getAnnotation(ExcelIgnore.class) == null;
    }

    private static String leafHead(ExcelProperty property, String fallback) {
        if (property == null || property.value().length == 0) {
            return fallback;
        }
        String[] heads = property.value();
        String leaf = heads[heads.length - 1];
        return leaf == null || leaf.isBlank() ? fallback : leaf;
    }

    /**
     * 无参构造器，没有则返回 {@code null}。
     * <p>
     * 这里不抛异常：只导出的类型（record、只有全参构造器的 VO）用不到构造器，
     * 缺了也该能正常导出；真正需要实例化时才在 {@link #instantiate()} 报错。
     */
    private static <T> Constructor<T> resolveConstructor(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            ReflectionUtils.makeAccessible(constructor);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
