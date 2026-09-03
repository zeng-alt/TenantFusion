package com.github.zeng.alt.tenant.hybrid;

import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.stream.Stream;

/**
 * 由固定列表支撑的 {@link ObjectProvider}。
 * <p>
 * {@code ObjectProvider#stream()} 的默认实现直接抛 {@code UnsupportedOperationException}，
 * 所以必须覆盖它，{@code orderedStream()} 才能用。
 *
 * @param <T> 元素类型
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
record ListObjectProvider<T>(List<T> elements) implements ObjectProvider<T> {

    @SafeVarargs
    static <T> ObjectProvider<T> of(T... elements) {
        return new ListObjectProvider<>(List.of(elements));
    }

    @Override
    public T getObject(Object... args) {
        return getObject();
    }

    @Override
    public T getObject() {
        if (elements.size() != 1) {
            throw new IllegalStateException("元素数量不为 1：" + elements.size());
        }
        return elements.get(0);
    }

    @Override
    public T getIfAvailable() {
        return elements.isEmpty() ? null : elements.get(0);
    }

    @Override
    public T getIfUnique() {
        return elements.size() == 1 ? elements.get(0) : null;
    }

    @Override
    public Stream<T> stream() {
        return elements.stream();
    }

    @Override
    public Stream<T> orderedStream() {
        return elements.stream();
    }
}
