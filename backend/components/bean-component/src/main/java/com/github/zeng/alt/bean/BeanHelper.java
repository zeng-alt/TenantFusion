package com.github.zeng.alt.bean;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * BeanUtils 增强工具，GraalVM Native Image 兼容。<br>
 * 批量拷贝请使用 MapStruct（编译期安全，性能更好）。
 *
 * @author zengJiaJun
 * @version 2.0
 */
public class BeanHelper extends BeanUtils {

    private BeanHelper() {}

    /**
     * 实例化一个类（需有无参构造器）。
     * 在 Native Image 中，目标类需要通过 RuntimeHints 注册反射方可使用。
     */
    public static Object instantiateBean(Class<?> clazz) {
        if (clazz == null || clazz.isInterface()) {
            return null;
        }
        return BeanUtils.instantiateClass(clazz);
    }

    /**
     * 通过 Supplier 实例化，避免 Native Image 反射问题。
     */
    public static Object instantiateBean(Supplier<?> supplier) {
        if (supplier == null) return null;
        return supplier.get();
    }

    // ========== copyToObject ==========

    public static <T> T copyToObject(Object source, Class<T> targetClz, Class<?> editable) {
        T target = BeanUtils.instantiateClass(targetClz);
        BeanUtils.copyProperties(source, target, editable);
        return target;
    }


    public static <T> T copyToObject(Object source, Class<T> targetClz, String... ignoreProperties) {
        T target = BeanUtils.instantiateClass(targetClz);
        BeanUtils.copyProperties(source, target, ignoreProperties);
        return target;
    }


    public static <S, T> T copyToObject(S source, Class<T> targetClz) {
        T target = BeanUtils.instantiateClass(targetClz);
        BeanUtils.copyProperties(source, target);
        return target;
    }


    public static <S, T> T copyToObject(S source, Class<T> targetClz, Consumer<T> consumer) {
        T target = BeanUtils.instantiateClass(targetClz);
        BeanUtils.copyProperties(source, target);
        if (consumer != null) {
            consumer.accept(target);
        }
        return target;
    }


    public static <S, T> T copyToObject(S source, Class<T> targetClz, BiConsumer<S, T> consumer) {
        T target = BeanUtils.instantiateClass(targetClz);
        BeanUtils.copyProperties(source, target);
        if (consumer != null) {
            consumer.accept(source, target);
        }
        return target;
    }

    // ========== copyToObject (Supplier) ==========

    public static <T> T copyToObject(Object source, Supplier<T> supplier, String... ignoreProperties) {
        T target = supplier.get();
        BeanUtils.copyProperties(source, target, ignoreProperties);
        return target;
    }

    public static <S, T> T copyToObject(S source, Supplier<T> supplier) {
        T target = supplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    public static <S, T> T copyToObject(S source, Supplier<T> supplier, Consumer<T> consumer) {
        T target = supplier.get();
        BeanUtils.copyProperties(source, target);
        if (consumer != null) {
            consumer.accept(target);
        }
        return target;
    }

    public static <S, T> T copyToObject(S source, Supplier<T> supplier, BiConsumer<S, T> consumer) {
        T target = supplier.get();
        BeanUtils.copyProperties(source, target);
        if (consumer != null) {
            consumer.accept(source, target);
        }
        return target;
    }

    // ========== copyToList ==========

    public static <S, T> List<T> copyToList(Iterable<S> source, Class<T> targetClz) {
        return copyToList(source, targetClz, (Class<?>) null);
    }

    public static <S, T> List<T> copyToList(Iterable<S> source, Class<T> targetClz, Class<?> editable) {
        if (source == null) return List.of();
        List<T> result = new ArrayList<>();
        for (S s : source) {
            T t = BeanUtils.instantiateClass(targetClz);
            BeanUtils.copyProperties(s, t, editable);
            result.add(t);
        }
        return result;
    }


    public static <S, T> List<T> copyToList(Iterable<S> source, Class<T> targetClz, String... ignoreProperties) {
        if (source == null) return List.of();
        List<T> result = new ArrayList<>();
        for (S s : source) {
            T t = BeanUtils.instantiateClass(targetClz);
            BeanUtils.copyProperties(s, t, ignoreProperties);
            result.add(t);
        }
        return result;
    }

    // ========== copyToList (Supplier) ==========

    public static <S, T> List<T> copyToList(Iterable<S> source, Supplier<T> supplier) {
        if (source == null) return List.of();
        List<T> result = new ArrayList<>();
        for (S s : source) {
            T t = supplier.get();
            BeanUtils.copyProperties(s, t);
            result.add(t);
        }
        return result;
    }
}