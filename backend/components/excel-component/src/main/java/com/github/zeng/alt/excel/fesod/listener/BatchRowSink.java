package com.github.zeng.alt.excel.fesod.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 攒够一批再下发，供 {@code ExcelReadSpec#consumeBatch(Consumer)} 使用。
 * <p>
 * 内存占用是「一批」而不是「整个文件」：批次交给下游后立刻新建缓冲，不复用
 * 同一个 {@code List}——下游很可能异步持有它，复用会让它看到后一批的数据。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class BatchRowSink<T> implements ExcelRowSink<T> {

    private final Consumer<List<T>> consumer;
    private final int batchSize;
    private List<T> buffer;
    private long count;

    public BatchRowSink(Consumer<List<T>> consumer, int batchSize) {
        this.consumer = consumer;
        this.batchSize = Math.max(1, batchSize);
        this.buffer = new ArrayList<>(this.batchSize);
    }

    @Override
    public void accept(T row) {
        buffer.add(row);
        count++;
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    @Override
    public void complete() {
        // 最后不足一批的余量也要下发，否则尾部数据会被静默丢掉
        flush();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public long count() {
        return count;
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<T> batch = buffer;
        buffer = new ArrayList<>(batchSize);
        consumer.accept(List.copyOf(batch));
    }
}
