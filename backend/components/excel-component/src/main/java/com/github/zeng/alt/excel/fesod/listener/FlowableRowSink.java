package com.github.zeng.alt.excel.fesod.listener;

import io.reactivex.rxjava3.core.FlowableEmitter;

/**
 * 把行下发到 {@code Flowable}，供 {@code ExcelReadSpec#stream()} 使用。
 * <p>
 * 背压说明：数据源是 POI 的 SAX 解析，不能按 {@code requested()} 暂停在任意行，
 * 因此用 {@code BackpressureStrategy.BUFFER} 由缓冲承担速度差；真正有用的是
 * {@link #isCancelled()}——下游取消后监听器会立刻停止解析剩余行，
 * 不会把整份文件读完。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class FlowableRowSink<T> implements ExcelRowSink<T> {

    private final FlowableEmitter<T> emitter;
    private long count;
    private boolean completed;

    public FlowableRowSink(FlowableEmitter<T> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void accept(T row) {
        if (emitter.isCancelled()) {
            return;
        }
        emitter.onNext(row);
        count++;
    }

    @Override
    public void complete() {
        completed = true;
        if (!emitter.isCancelled()) {
            emitter.onComplete();
        }
    }

    /**
     * 是否已经正常终结。
     * <p>
     * 调用方据此避免在 {@code onComplete} 之后再发 {@code onError}——
     * 那会变成 RxJava 的 undeliverable error，只污染日志。
     *
     * @return true 表示已 onComplete
     */
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean isCancelled() {
        return emitter.isCancelled();
    }

    @Override
    public long count() {
        return count;
    }
}
