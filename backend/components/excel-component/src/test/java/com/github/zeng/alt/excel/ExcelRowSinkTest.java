package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.fesod.listener.BatchRowSink;
import com.github.zeng.alt.excel.fesod.listener.PredicateRowSink;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 消费计数仅包含已成功完成的业务回调。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelRowSinkTest {

    @Test
    void failedBatchDoesNotIncreaseSuccessfulCount() {
        final AtomicInteger calls = new AtomicInteger();
        final BatchRowSink<Integer> sink = new BatchRowSink<>(rows -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("database unavailable");
            }
        }, 2);
        sink.accept(1);
        sink.accept(2);
        sink.accept(3);

        assertThatThrownBy(() -> sink.accept(4)).isInstanceOf(IllegalStateException.class);
        assertThat(sink.count()).isEqualTo(2L);
    }

    @Test
    void failedPredicateDoesNotIncreaseSuccessfulCount() {
        final PredicateRowSink<Integer> sink = new PredicateRowSink<>(row -> {
            throw new IllegalStateException("database unavailable");
        });

        assertThatThrownBy(() -> sink.accept(1)).isInstanceOf(IllegalStateException.class);
        assertThat(sink.count()).isZero();
    }
}
