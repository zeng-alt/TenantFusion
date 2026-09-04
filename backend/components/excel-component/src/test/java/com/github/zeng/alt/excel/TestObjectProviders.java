package com.github.zeng.alt.excel;

import org.springframework.beans.factory.ObjectProvider;

import java.util.stream.Stream;

/**
 * 单元测试里手工构造 {@link ObjectProvider} 的便利类。
 * <p>
 * {@code FesodExcelContext} 的协作者一律是 {@link ObjectProvider}（为了避开与
 * MVC 基础设施的循环依赖），不起 Spring 上下文时需要自己造。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
final class TestObjectProviders {

    private TestObjectProviders() {
    }

    /**
     * 空 provider。
     *
     * @param <T> 元素类型
     * @return 取不到任何对象的 provider
     */
    static <T> ObjectProvider<T> empty() {
        return of(null);
    }

    /**
     * 只含一个对象的 provider。
     *
     * @param instance 对象，可为 {@code null}
     * @param <T>      元素类型
     * @return provider
     */
    static <T> ObjectProvider<T> of(T instance) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                if (instance == null) {
                    throw new UnsupportedOperationException("no instance");
                }
                return instance;
            }

            @Override
            public T getIfAvailable() {
                return instance;
            }

            @Override
            public T getIfUnique() {
                return instance;
            }

            @Override
            public Stream<T> stream() {
                return instance == null ? Stream.empty() : Stream.of(instance);
            }

            @Override
            public Stream<T> orderedStream() {
                return stream();
            }
        };
    }
}
