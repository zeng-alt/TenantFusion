package com.github.zeng.alt.excel.fesod.listener;

import java.util.function.Predicate;

/**
 * 逐行交给一个可中止的谓词，供 {@code ExcelReadSpec#consumeWhile(Predicate)} 使用。
 * <p>
 * 谓词返回 {@code false} 即视为下游不再需要数据，监听器随后停止解析剩余行。
 * 响应式适配（{@code RxExcel.stream}）就是靠这条路把订阅者的取消信号传下来的——
 * 因此本模块的核心 SPI 不需要出现任何 RxJava 类型。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class PredicateRowSink<T> implements ExcelRowSink<T> {

    private final Predicate<T> consumer;
    private long count;
    private boolean cancelled;

    public PredicateRowSink(Predicate<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(T row) {
        if (cancelled) {
            return;
        }
        count++;
        if (!consumer.test(row)) {
            cancelled = true;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public long count() {
        return count;
    }
}
