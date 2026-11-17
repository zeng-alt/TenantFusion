package com.github.zeng.alt.json.jackson3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 通过 Vavr 工厂重建不可变集合，不依赖实现类反射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class VavrCollectionHelper {
    private VavrCollectionHelper() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Object collection(Class<?> raw, List<Object> elements) {
        if (io.vavr.collection.CharSeq.class.isAssignableFrom(raw)) {
            return io.vavr.collection.CharSeq.ofAll((Iterable) elements);
        }
        if (io.vavr.collection.Array.class.isAssignableFrom(raw)) {
            return io.vavr.collection.Array.ofAll(elements);
        }
        if (io.vavr.collection.Vector.class.isAssignableFrom(raw)) {
            return io.vavr.collection.Vector.ofAll(elements);
        }
        if (io.vavr.collection.Stream.class.isAssignableFrom(raw)) {
            return io.vavr.collection.Stream.ofAll(elements);
        }
        if (io.vavr.collection.Queue.class.isAssignableFrom(raw)) {
            return io.vavr.collection.Queue.ofAll(elements);
        }
        if (io.vavr.collection.PriorityQueue.class.isAssignableFrom(raw)) {
            return io.vavr.collection.PriorityQueue.ofAll((Comparator) Comparator.naturalOrder(), elements);
        }
        if (io.vavr.collection.SortedSet.class.isAssignableFrom(raw)) {
            return io.vavr.collection.TreeSet.ofAll((Comparator) Comparator.naturalOrder(), elements);
        }
        if (io.vavr.collection.LinkedHashSet.class.isAssignableFrom(raw)) {
            return io.vavr.collection.LinkedHashSet.ofAll(elements);
        }
        if (io.vavr.collection.Set.class.isAssignableFrom(raw)) {
            return io.vavr.collection.HashSet.ofAll(elements);
        }
        return io.vavr.collection.List.ofAll(elements);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Object map(Class<?> raw, Map<Object, Object> entries) {
        if (io.vavr.collection.SortedMap.class.isAssignableFrom(raw)) {
            return io.vavr.collection.TreeMap.ofAll((Comparator) Comparator.naturalOrder(), entries);
        }
        if (io.vavr.collection.LinkedHashMap.class.isAssignableFrom(raw)) {
            return io.vavr.collection.LinkedHashMap.ofAll(entries);
        }
        return io.vavr.collection.HashMap.ofAll(entries);
    }
}
