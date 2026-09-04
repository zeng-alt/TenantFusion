package com.github.zeng.alt.excel.fesod.listener;

import java.util.function.Consumer;

/**
 * 逐行交给调用方的 {@code Consumer}，供 {@code ExcelReadSpec#consume(Consumer)} 使用。
 * <p>
 * 不持有任何行，内存占用与文件大小无关。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ConsumerRowSink<T> implements ExcelRowSink<T> {

    private final Consumer<T> consumer;
    private long count;

    public ConsumerRowSink(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(T row) {
        consumer.accept(row);
        count++;
    }

    @Override
    public long count() {
        return count;
    }
}
