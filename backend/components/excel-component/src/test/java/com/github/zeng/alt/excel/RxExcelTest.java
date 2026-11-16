package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.rx.RxExcel;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 响应式读取的需求控制与写出失败的取消行为。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RxExcelTest {

    @Test
    void parsingWaitsForDemandAndCancellationReleasesWorker() throws Exception {
        final AtomicInteger parsed = new AtomicInteger();
        final CountDownLatch finished = new CountDownLatch(1);
        final TestSubscriber<Integer> subscriber = RxExcel.stream(source(parsed, finished)).test(0);
        try {
            await().until(() -> parsed.get() > 0);
            assertThat(finished.await(100, TimeUnit.MILLISECONDS)).isFalse();
            assertThat(parsed).hasValue(1);
            subscriber.assertNoValues();

            subscriber.requestMore(3).awaitCount(3).assertValues(0, 1, 2);
            assertThat(parsed.get()).isLessThanOrEqualTo(4);
        } finally {
            subscriber.cancel();
        }
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeFailureCancelsUpstream() {
        final ExcelWriteSpec<Integer> spec = mock(ExcelWriteSpec.class);
        final AtomicBoolean cancelled = new AtomicBoolean();
        when(spec.write(any(java.util.Iterator.class))).thenReturn(Try.failure(new IllegalStateException("disk full")));

        final Try<Long> result = RxExcel.write(spec, Flowable.range(0, 100_000).doOnCancel(() -> cancelled.set(true)));

        assertThat(result.isFailure()).isTrue();
        assertThat(cancelled).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static ExcelReadSpec<Integer> source(AtomicInteger parsed, CountDownLatch finished) {
        final ExcelReadSpec<Integer> spec = mock(ExcelReadSpec.class);
        when(spec.consumeWhile(any())).thenAnswer(invocation -> {
            final Predicate<Integer> consumer = invocation.getArgument(0);
            try {
                for (int index = 0; index < 1000; index++) {
                    parsed.incrementAndGet();
                    if (!consumer.test(index)) {
                        break;
                    }
                }
                return Try.success((long) parsed.get());
            } finally {
                finished.countDown();
            }
        });
        return spec;
    }
}
